let NUM_DIMENSIONS = BigInt(4);
let BLOCK_MESSAGE_TYPE_PROBE_REGIONS = BigInt(1);
let BLOCK_MESSAGE_TYPE_DESCRIBE_REGIONS = BigInt(2);
let CHUNK_SIZE = BigInt(16);
let READ_FLAG_MASK = BigInt(1);
let SUBSCRIBE_FLAG_MASK = BigInt(2);
let BLOCK_SIZE_NO_EXIST = BigInt(-1);
function get_byte_machine_byte_order_list_int64() {
  let test_number = BigInt('0x1122334455667788');
  let buffer = new ArrayBuffer(8);
  let uint_8_array = new Uint8Array(buffer);
  let int_64_array = new BigInt64Array(buffer, 0, 1);
  int_64_array[0] = test_number;

  //console.log(uint_8_array);
  //console.log(int_64_array);
  let expected_bytes_least_to_greatest = [0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88];
  let observed_offsets = [];
  for (let i = 0; i < expected_bytes_least_to_greatest.length; i++) {
    observed_offsets.push(uint_8_array.indexOf(expected_bytes_least_to_greatest[i]));
  }
  return observed_offsets;
}
let machine_byte_orderings_for_int64 = get_byte_machine_byte_order_list_int64();
function positive_modulo(a, b) {
  return (a % b + b) % b;
}
function big_int_min(a, b) {
  return a < b ? a : b;
}
function big_int_max(a, b) {
  return a > b ? a : b;
}
function big_int_divide_and_floor(numerator, denominator) {
  if (numerator < 0) {
    var positive_numerator = BigInt(-1) * numerator;
    var r = positive_numerator / denominator;
    var rounding = positive_numerator % denominator == BigInt(0) ? BigInt(0) : BigInt(1);
    return -(r + rounding);
  } else {
    if (typeof numerator !== "bigint" || typeof denominator !== "bigint") {
      throw new Error("numerator or denominator was not bigint.");
    } else {
      return numerator / denominator;
    }
  }
}
function block_coord_to_cuboid_address(c) {
  /*  Returns the cuboid address of the chunk that should contain the specified block. */
  return new CuboidAddress(new Coordinate([big_int_divide_and_floor(c.getX(), CHUNK_SIZE) * CHUNK_SIZE, big_int_divide_and_floor(c.getY(), CHUNK_SIZE) * CHUNK_SIZE, big_int_divide_and_floor(c.getZ(), CHUNK_SIZE) * CHUNK_SIZE, BigInt(0)]), new Coordinate([big_int_divide_and_floor(c.getX(), CHUNK_SIZE) * CHUNK_SIZE + CHUNK_SIZE, big_int_divide_and_floor(c.getY(), CHUNK_SIZE) * CHUNK_SIZE + CHUNK_SIZE, big_int_divide_and_floor(c.getZ(), CHUNK_SIZE) * CHUNK_SIZE + CHUNK_SIZE, BigInt(0) + BigInt(1)]));
}
class DynamicArrayBuffer {
  constructor() {
    this.available_capacity = 1;
    this.buffer = new ArrayBuffer(this.available_capacity);
    this.used_capacity = 0;
    this.read_position = 0;
  }
  static from_array_buffer(ab) {
    let dab = new DynamicArrayBuffer();
    dab.buffer = new ArrayBuffer(ab.byteLength);
    new Uint8Array(dab.buffer, 0, ab.byteLength).set(new Uint8Array(ab), 0);
    dab.available_capacity = ab.byteLength;
    dab.used_capacity = ab.byteLength;
    return dab;
  }
  increase_buffer_size(required_capacity) {
    let new_capacity = this.available_capacity;
    while (new_capacity < required_capacity) {
      new_capacity *= 2;
      //console.log("Increased capacity to " + new_capacity);
    }
    let new_array_buffer = new ArrayBuffer(new_capacity);
    new Uint8Array(new_array_buffer, 0, new_capacity).set(new Uint8Array(this.buffer), 0);
    this.buffer = new_array_buffer;
    this.available_capacity = new_capacity;
  }
  get_used_buffer() {
    /* Return an array buffer that is exactly the size of the required data. */
    let rtn_buffer = new ArrayBuffer(this.used_capacity);
    new Uint8Array(rtn_buffer, 0, this.used_capacity).set(new Uint8Array(this.buffer, 0, this.used_capacity), 0);
    return rtn_buffer;
  }
  write_bytes(ba) {
    let num_bytes_to_add = ba.byteLength;
    let required_capacity = this.used_capacity + num_bytes_to_add;
    if (required_capacity > this.available_capacity) {
      this.increase_buffer_size(required_capacity);
    }
    new Uint8Array(this.buffer, 0, this.available_capacity).set(new Uint8Array(ba), this.used_capacity);
    this.used_capacity += num_bytes_to_add;
    //console.log("this.used_capacity=" + this.used_capacity + " this.available_capacity=" + this.available_capacity);
    //console.log(new Uint8Array(this.buffer));
  }
  write_int_64(i) {
    let ba_for_number = new ArrayBuffer(8);
    let z = new BigInt64Array(ba_for_number, 0, 1);
    z[0] = i;
    let uint_8_original = new Uint8Array(ba_for_number);

    /*  In Java, all long types are big-endian regardless of underlying machine endianness. */
    let ba_endian_corrected = new ArrayBuffer(8);
    let uint_8_endian_corrected = new Uint8Array(ba_endian_corrected);
    uint_8_endian_corrected[0] = uint_8_original[machine_byte_orderings_for_int64[0]]; /*  'Most significant' is first in big endian. */
    uint_8_endian_corrected[1] = uint_8_original[machine_byte_orderings_for_int64[1]];
    uint_8_endian_corrected[2] = uint_8_original[machine_byte_orderings_for_int64[2]];
    uint_8_endian_corrected[3] = uint_8_original[machine_byte_orderings_for_int64[3]];
    uint_8_endian_corrected[4] = uint_8_original[machine_byte_orderings_for_int64[4]];
    uint_8_endian_corrected[5] = uint_8_original[machine_byte_orderings_for_int64[5]];
    uint_8_endian_corrected[6] = uint_8_original[machine_byte_orderings_for_int64[6]];
    uint_8_endian_corrected[7] = uint_8_original[machine_byte_orderings_for_int64[7]];
    this.write_bytes(ba_endian_corrected);
  }
  read_n_bytes(n) {
    let num_bytes_to_read = Number(n);
    let actual_data = new ArrayBuffer(num_bytes_to_read);
    new Uint8Array(actual_data, 0, num_bytes_to_read).set(new Uint8Array(this.buffer, this.read_position, num_bytes_to_read), 0);
    this.read_position += num_bytes_to_read;
    return actual_data;
  }
  read_n_int_64s(n) {
    let bytes_required = 8 * n;
    let source_array_buffer = this.buffer;
    let source_bytes = new Uint8Array(source_array_buffer, this.read_position, bytes_required);
    let destination_array_buffer = new ArrayBuffer(bytes_required);
    let destination_bytes = new Uint8Array(destination_array_buffer);
    let z = new BigInt64Array(destination_array_buffer, 0, n);

    /*  In Java, all long types are big-endian regardless of underlying machine endianness. */
    for (let i = 0; i < n; i++) {
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[0]] = source_bytes[i * 8 + 0]; /*  'Most significant' is first in big endian. */
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[1]] = source_bytes[i * 8 + 1];
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[2]] = source_bytes[i * 8 + 2];
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[3]] = source_bytes[i * 8 + 3];
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[4]] = source_bytes[i * 8 + 4];
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[5]] = source_bytes[i * 8 + 5];
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[6]] = source_bytes[i * 8 + 6];
      destination_bytes[i * 8 + machine_byte_orderings_for_int64[7]] = source_bytes[i * 8 + 7];
    }
    this.read_position += bytes_required;
    return z;
  }
  read_one_int_64() {
    let z = this.read_n_int_64s(1);
    return z[0];
  }
}
class ChunkModel {
  constructor() {
    this.available_chunks = {}, this.pending_requested_chunks = {};
  }
  get_block_at_address(coordinate) {
    let cuboid_address = block_coord_to_cuboid_address(coordinate);
    let chunk_key = cuboid_address.to_string();
    let block_data = this.available_chunks.hasOwnProperty(chunk_key) ? this.available_chunks[chunk_key][coordinate.getX() + ""][coordinate.getY() + ""][coordinate.getZ() + ""][BigInt(0) + ""] : null;
    return block_data;
  }
  update_chunk_model_with_cuboids(cuboids) {
    let rtn = [];
    for (let i = 0; i < cuboids.length; i++) {
      let cuboid_address = cuboids[i].cuboid_address;
      let lower_address = cuboid_address.get_canonical_lower_coordinate();
      let upper_address = cuboid_address.get_canonical_upper_coordinate();
      let chunk_key = cuboid_address.to_string();
      rtn.push(i + ") " + cuboid_address.to_string() + " Chunk key: " + chunk_key);
      if (this.pending_requested_chunks.hasOwnProperty(chunk_key)) {
        rtn.push("Got describe region for chunk, remove from pending list:" + chunk_key);
        delete this.pending_requested_chunks[chunk_key];
        this.available_chunks[chunk_key] = {};
        for (let x = lower_address.values[0]; x < upper_address.values[0]; x++) {
          if (!this.available_chunks[chunk_key].hasOwnProperty(x + "")) {
            this.available_chunks[chunk_key][x + ""] = {};
          }
          for (let y = lower_address.values[1]; y < upper_address.values[1]; y++) {
            if (!this.available_chunks[chunk_key][x + ""].hasOwnProperty(y + "")) {
              this.available_chunks[chunk_key][x + ""][y + ""] = {};
            }
            for (let z = lower_address.values[2]; z < upper_address.values[2]; z++) {
              this.available_chunks[chunk_key][x + ""][y + ""][z + ""] = {};
              this.available_chunks[chunk_key][x + ""][y + ""][z + ""][BigInt(0) + ""] = cuboids[i].get_data_for_one_block(x, y, z);
            }
          }
        }
      } else {
        rtn.push("Got describe region for chunk that was not requestd. Must be from update on server: =" + chunk_key);
        if (!this.available_chunks.hasOwnProperty(chunk_key)) {
          this.available_chunks[chunk_key] = {};
        }
        for (let x = lower_address.values[0]; x < upper_address.values[0]; x++) {
          if (!this.available_chunks[chunk_key].hasOwnProperty(x + "")) {
            this.available_chunks[chunk_key][x + ""] = {};
          }
          for (let y = lower_address.values[1]; y < upper_address.values[1]; y++) {
            if (!this.available_chunks[chunk_key][x + ""].hasOwnProperty(y + "")) {
              this.available_chunks[chunk_key][x + ""][y + ""] = {};
            }
            for (let z = lower_address.values[2]; z < upper_address.values[2]; z++) {
              let current_block_chunk_key = block_coord_to_cuboid_address(new Coordinate([BigInt(x), BigInt(y), BigInt(z), BigInt(0)])).to_string();
              if (this.available_chunks.hasOwnProperty(current_block_chunk_key)) {
                this.available_chunks[current_block_chunk_key][x + ""][y + ""][z + ""] = {};
                this.available_chunks[current_block_chunk_key][x + ""][y + ""][z + ""][BigInt(0) + ""] = cuboids[i].get_data_for_one_block(x, y, z);
              } else {
                rtn.push("Discarding update for block x=" + x + ", y=" + y + ", z=" + z + " because it was in an unloaded chunk.");
              }
            }
          }
        }
      }
    }
    return rtn;
  }
}
g_chunk_model = new ChunkModel();
class Coordinate {
  constructor(values) {
    if (Array.isArray(values)) {
      this.values = values;
    } else {
      throw new Error("values was not an array.");
    }
  }
  getX() {
    return this.get_value_at_index(0);
  }
  getY() {
    return this.get_value_at_index(1);
  }
  getZ() {
    return this.get_value_at_index(2);
  }
  get_num_dimensions() {
    return BigInt(this.values.length);
  }
  write_into_buffer(dab, num_dimensions) {
    for (let i = 0; i < num_dimensions; i++) {
      dab.write_int_64(this.values[i]);
    }
  }
  get_value_at_index(i) {
    if (i < this.values.length) {
      return this.values[i];
    } else {
      return BigInt(0);
    }
  }
  static read_from_buffer(dab, num_dimensions) {
    let vals = [];
    for (let i = 0; i < num_dimensions; i++) {
      let v = dab.read_one_int_64();
      console.log(v);
      vals.push(v);
    }
    return new Coordinate(vals);
  }
  to_string() {
    return "(" + this.values.join(", ") + ")";
  }
}
class CuboidAddress {
  constructor(a, b) {
    this.a = a;
    this.b = b;
  }
  write_into_buffer(dab, num_dimensions) {
    this.a.write_into_buffer(dab, num_dimensions);
    this.b.write_into_buffer(dab, num_dimensions);
  }
  static read_from_buffer(dab, num_dimensions) {
    let a = Coordinate.read_from_buffer(dab, num_dimensions);
    let b = Coordinate.read_from_buffer(dab, num_dimensions);
    return new CuboidAddress(a, b);
  }
  get_canonical_lower_coordinate() {
    let canonical_values = [];
    for (let i = 0; i < big_int_max(this.a.values.length, this.b.values.length); i++) {
      let c = this.a.get_value_at_index(i);
      let d = this.b.get_value_at_index(i);
      canonical_values.push(big_int_min(c, d));
    }
    return new Coordinate(canonical_values);
  }
  get_canonical_upper_coordinate() {
    let canonical_values = [];
    for (let i = 0; i < big_int_max(this.a.values.length, this.b.values.length); i++) {
      let c = this.a.get_value_at_index(i);
      let d = this.b.get_value_at_index(i);
      canonical_values.push(big_int_max(c, d));
    }
    return new Coordinate(canonical_values);
  }
  get_width_for_index(index) {
    let lower = this.get_canonical_lower_coordinate();
    let upper = this.get_canonical_upper_coordinate();
    return upper.values[index] - lower.values[index];
  }
  volume() {
    let lower_address = this.get_canonical_lower_coordinate();
    let upper_address = this.get_canonical_upper_coordinate();
    let rtn = BigInt(1);
    for (let i = 0; i < lower_address.values.length; i++) {
      rtn *= this.get_width_for_index(i);
    }
    return rtn;
  }
  get_linear_array_index_for_coordinate(coordinate) {
    let lower = this.get_canonical_lower_coordinate();
    let total_index = BigInt(0);
    let dimension_value = BigInt(1);
    for (let i = BigInt(0); i < coordinate.get_num_dimensions(); i++) {
      let dimension_offset = coordinate.get_value_at_index(i) - lower.get_value_at_index(i);
      if (dimension_offset < BigInt(0)) {
        alert("dimension_offset was negative? :" + dimension_offset);
      }
      total_index += dimension_offset * dimension_value;
      dimension_value *= this.get_width_for_index(i);
    }
    return total_index;
  }
  to_string() {
    return this.a.to_string() + " -> " + this.b.to_string();
  }
}
class CuboidDataLengths {
  constructor(length_values, data_offsets, total_size) {
    this.length_values = length_values;
    this.data_offsets = data_offsets;
    this.total_size = total_size;
  }
  write_into_buffer(dab) {
    for (let i = 0; i < this.length_values.length; i++) {
      console.log("this.length:" + this.length_values[i]);
      dab.write_int_64(this.length_values[i]);
    }
  }
  static read_from_buffer(dab, cuboid_address) {
    let length_values = dab.read_n_int_64s(Number(cuboid_address.volume()));
    let data_offsets = new BigInt64Array(length_values.length);
    let total = BigInt(0);
    for (let i = 0; i < length_values.length; i++) {
      data_offsets[i] = total;
      if (length_values[i] > BigInt(0)) {
        total += length_values[i];
      }
    }
    return new CuboidDataLengths(length_values, data_offsets, total);
  }
  static from_array_of_array_buffers(array_of_array_buffers) {
    let length_values = new BigInt64Array(array_of_array_buffers.length);
    let data_offsets = new BigInt64Array(array_of_array_buffers.length);
    let total = BigInt(0);
    for (let i = 0; i < array_of_array_buffers.length; i++) {
      length_values[i] = BigInt(array_of_array_buffers[i].byteLength);
      data_offsets[i] = total;
      if (length_values[i] > BigInt(0)) {
        total += length_values[i];
      }
    }
    return new CuboidDataLengths(length_values, data_offsets, total);
  }
  get_total_size() {
    return this.total_size;
  }
}
class CuboidData {
  constructor(aggregate_block_data) {
    this.aggregate_block_data = aggregate_block_data;
  }
  write_into_buffer(dab) {
    console.log("this.aggregate_block_data:" + this.aggregate_block_data);
    dab.write_bytes(this.aggregate_block_data);
  }
  static read_from_buffer(dab, cuboid_data_lengths) {
    let aggregate_block_data = dab.read_n_bytes(cuboid_data_lengths.get_total_size());
    return new CuboidData(aggregate_block_data);
  }
  static from_array_of_array_buffers(array_of_array_buffers) {
    let dab = new DynamicArrayBuffer();
    for (let i = 0; i < array_of_array_buffers.length; i++) {
      dab.write_bytes(array_of_array_buffers[i]);
    }
    return new CuboidData(dab.get_used_buffer());
  }
}
class Cuboid {
  constructor(cuboid_address, cuboid_data_lengths, cuboid_data) {
    this.cuboid_address = cuboid_address;
    this.cuboid_data_lengths = cuboid_data_lengths;
    this.cuboid_data = cuboid_data;
  }
  get_data_for_one_block(x, y, z) {
    let index_of_block = this.cuboid_address.get_linear_array_index_for_coordinate(new Coordinate([x, y, z, BigInt(0)]));
    let size_of_block = Number(this.cuboid_data_lengths.length_values[index_of_block]);
    let index_of_data = Number(this.cuboid_data_lengths.data_offsets[index_of_block]);
    if (size_of_block > 0) {
      let aggregate_block_data = new ArrayBuffer(size_of_block);
      new Uint8Array(aggregate_block_data, 0, size_of_block).set(new Uint8Array(this.cuboid_data.aggregate_block_data, index_of_data, size_of_block), 0);
      let text = new TextDecoder("utf-8").decode(aggregate_block_data);
      console.log("Block size is " + size_of_block + " from index of block: " + index_of_block + " data index " + index_of_data + " at " + x + "," + y + "," + z + " = " + text);
      return {
        "size": size_of_block,
        "data": aggregate_block_data
      };
    } else {
      return {
        "size": size_of_block,
        "data": new ArrayBuffer(0)
      };
    }
  }
}
class ProbeRegionsRequestMessage {
  constructor(cuboid_address_list, do_read, do_subscribe) {
    this.cuboid_address_list = cuboid_address_list;
    this.flags = BigInt(0);
    if (do_read) {
      this.flags |= READ_FLAG_MASK;
    }
    if (do_subscribe) {
      this.flags |= SUBSCRIBE_FLAG_MASK;
    }
  }
  get_message_type() {
    return BLOCK_MESSAGE_TYPE_PROBE_REGIONS;
  }
  as_array_buffer() {
    let dab = new DynamicArrayBuffer();
    dab.write_int_64(this.get_message_type());
    dab.write_int_64(BigInt(12345)); // TODO:  Actual conversation ID
    dab.write_int_64(this.flags);
    dab.write_int_64(NUM_DIMENSIONS);
    dab.write_int_64(BigInt(this.cuboid_address_list.length));
    for (var i = 0; i < this.cuboid_address_list.length; i++) {
      this.cuboid_address_list[i].write_into_buffer(dab, NUM_DIMENSIONS);
    }
    return dab.get_used_buffer();
  }
}
class DescribeRegionsMessage {
  constructor(cuboids, num_dimensions) {
    this.cuboids = cuboids;
    this.num_dimensions = num_dimensions;
  }
  as_array_buffer() {
    let dab = new DynamicArrayBuffer();
    dab.write_int_64(BLOCK_MESSAGE_TYPE_DESCRIBE_REGIONS);
    dab.write_int_64(BigInt(12345)); // TODO:  Actual conversation ID
    dab.write_int_64(NUM_DIMENSIONS);
    dab.write_int_64(BigInt(this.cuboids.length));
    for (let i = 0; i < this.cuboids.length; i++) {
      this.cuboids[i].cuboid_address.write_into_buffer(dab, NUM_DIMENSIONS);
    }
    for (let i = 0; i < this.cuboids.length; i++) {
      this.cuboids[i].cuboid_data_lengths.write_into_buffer(dab);
    }
    for (let i = 0; i < this.cuboids.length; i++) {
      this.cuboids[i].cuboid_data.write_into_buffer(dab);
    }
    return dab.get_used_buffer();
  }
  static from_byte_array(ba) {
    let dab = DynamicArrayBuffer.from_array_buffer(ba);
    let message_type_int = dab.read_one_int_64();
    let conversation_id = dab.read_one_int_64(); // TODO: Actually do something with this.
    let num_dimensions = dab.read_one_int_64();
    let num_regions = dab.read_one_int_64();
    let cuboid_addresses = [];
    for (var i = 0; i < num_regions; i++) {
      cuboid_addresses.push(CuboidAddress.read_from_buffer(dab, num_dimensions));
    }
    let cuboid_data_lengths = [];
    for (var i = 0; i < cuboid_addresses.length; i++) {
      cuboid_data_lengths.push(CuboidDataLengths.read_from_buffer(dab, cuboid_addresses[i]));
    }
    let cuboid_data = [];
    for (var i = 0; i < cuboid_addresses.length; i++) {
      cuboid_data.push(CuboidData.read_from_buffer(dab, cuboid_data_lengths[i]));
    }
    let cuboids = [];
    for (var i = 0; i < cuboid_addresses.length; i++) {
      cuboids.push(new Cuboid(cuboid_addresses[i], cuboid_data_lengths[i], cuboid_data[i]));
    }
    return new DescribeRegionsMessage(cuboids, num_dimensions);
  }
}
document.addEventListener("DOMContentLoaded", function (e) {
  function get_camera_viewport_coordinates(m) {
    return {
      "min_x": m.camera_center_x - m.camera_outer_x_size / BigInt(2),
      "max_x": m.camera_center_x + m.camera_outer_x_size / BigInt(2),
      "min_z": m.camera_center_z - m.camera_outer_z_size / BigInt(2),
      "max_z": m.camera_center_z + m.camera_outer_z_size / BigInt(2)
    };
  }
  class IndividualBlock extends React.Component {
    constructor(props) {
      super(props);
    }
    componentDidMount() {}
    getBlockValue(coordinate) {
      let viewport_block_key = "viewport_" + this.props.viewport_x + "_" + this.props.viewport_z;
      if (this.props.top_level_state.hasOwnProperty(viewport_block_key)) {
        var viewport_block_state = this.props.top_level_state[viewport_block_key];
        if (viewport_block_state == "set") {
          var individual_block_details = g_chunk_model.get_block_at_address(coordinate);
          if (individual_block_details == null) {
            return "Block was null?";
          } else {
            return new TextDecoder("utf-8").decode(individual_block_details["data"]);
          }
        } else {
          return viewport_block_state;
        }
      } else {
        return "nil";
      }
    }
    renderBlockValue(blockValue) {
      let block_skins = {
        "CAS:1317-65-3": "🪨",
        "GRIN:28551": "🪵"
      };
      if (blockValue in block_skins) {
        return block_skins[blockValue];
      } else {
        return blockValue;
      }
    }
    render() {
      var classname = this.props.actor_z == this.props.block_z && this.props.actor_x == this.props.block_x ? "selected" : "";
      return /*#__PURE__*/React.createElement("td", {
        class: classname
      }, this.renderBlockValue(this.getBlockValue(new Coordinate([BigInt(this.props.block_x), BigInt(this.props.block_y), BigInt(this.props.block_z), BigInt(0)]))));
    }
  }
  class BlockGrid extends React.Component {
    constructor(props) {
      super(props);
    }
    componentDidMount() {}
    //getBlockValue(x,y,z){
    //	let cuboid_address = block_coord_to_cuboid_address(x, y, z);
    //	let chunk_key = cuboid_address.to_string();
    //	let block_data = g_chunk_model.available_chunks.hasOwnProperty(chunk_key) ? g_chunk_model.available_chunks[chunk_key][x+""][y+""][z+""] : null;
    //
    //	let presented_size = "";// block_data["size"] + ":";
    //
    //	return block_data == null ? "null" : presented_size + (new TextDecoder("utf-8").decode(block_data["data"]));
    //}
    render() {
      var rows = [];
      var view = get_camera_viewport_coordinates(this.props);
      for (var i = view.max_z; i >= view.min_z; i--) {
        var columns = [];
        for (var j = view.min_x; j <= view.max_x; j++) {
          columns.push( /*#__PURE__*/React.createElement(IndividualBlock, {
            block_x: j,
            block_y: this.props.y,
            block_z: i,
            actor_x: this.props.x,
            actor_y: this.props.y,
            actor_z: this.props.z,
            viewport_x: j - view.min_x,
            viewport_z: i - view.min_z,
            top_level_state: this.props.top_level_state
          }));
        }
        rows.push( /*#__PURE__*/React.createElement("tr", null, columns));
      }
      return /*#__PURE__*/React.createElement("table", {
        cellspacing: "0",
        class: "block-grid-table"
      }, /*#__PURE__*/React.createElement("tbody", null, rows));
    }
  }
  class HomePageUI extends React.Component {
    constructor(props) {
      super(props);
      let initial_state = {
        x: BigInt(0),
        y: BigInt(0),
        z: BigInt(0),
        camera_center_x: BigInt(0),
        camera_center_z: BigInt(0),
        camera_outer_x_size: BigInt(30),
        camera_outer_z_size: BigInt(30),
        websocket_url: "ws://127.0.0.1:8888/block-manager",
        websocket: undefined,
        output_lines: [],
        input_value: "CAS:1317-65-3"
      };
      this.state = initial_state;
    }
    componentDidMount() {
      this.onConnect();
      document.onkeydown = e => this.onKeyDown(e || window.event);
    }
    enumerateRequiredChunks() {
      var view = get_camera_viewport_coordinates(this.state);
      var required_chunks = {};
      for (var i = view.max_z; i >= view.min_z; i -= BigInt(1)) {
        for (var j = view.min_x; j <= view.max_x; j += BigInt(1)) {
          let cuboid_address = block_coord_to_cuboid_address(new Coordinate([BigInt(j), BigInt(this.state.y), BigInt(i), BigInt(0)]));
          let chunk_key = cuboid_address.to_string();
          required_chunks[chunk_key] = cuboid_address;
        }
      }
      return required_chunks;
    }
    sendChunkLoadRequest(chunks_coords_to_load) {
      if (chunks_coords_to_load.length == 0) {
        this.addOutputLine("No chunks to load.");
      } else {
        let cuboid_address_list = [];
        for (var i = 0; i < chunks_coords_to_load.length; i++) {
          cuboid_address_list.push(chunks_coords_to_load[i]);
        }
        let do_read = true;
        let do_subscribe = true;
        let read_regions_request_message = new ProbeRegionsRequestMessage(cuboid_address_list, do_read, do_subscribe);
        var buffer_to_send = read_regions_request_message.as_array_buffer();
        console.log(new Uint8Array(buffer_to_send));
        this.state.websocket.send(buffer_to_send);
      }
    }
    updateViewportFlags() {
      let view = get_camera_viewport_coordinates(this.state);
      let new_viewport_flags = {};
      for (var i = view.max_z; i >= view.min_z; i--) {
        var columns = [];
        for (var j = view.min_x; j <= view.max_x; j++) {
          let viewport_x = j - view.min_x;
          let viewport_y = this.state.y;
          let viewport_z = i - view.min_z;
          let block_x = j;
          let block_y = this.state.y;
          let block_z = i;
          let viewport_block_key = "viewport_" + viewport_x + "_" + viewport_z;
          let cuboid_address = block_coord_to_cuboid_address(new Coordinate([BigInt(block_x), BigInt(block_y), BigInt(block_z)]));
          let chunk_key = cuboid_address.to_string();
          if (g_chunk_model.available_chunks.hasOwnProperty(chunk_key)) {
            new_viewport_flags[viewport_block_key] = "set";
          } else {
            /*  No data for this chunk, must be pending. */
            new_viewport_flags[viewport_block_key] = "PEN";
          }
        }
      }
      this.setState(new_viewport_flags);
    }
    loadChunksAndEvictStaleOnes(chunks_to_load, stale_chunks) {
      var chunks_coords_to_load = [];
      for (const [chunk_key, chunk_coords] of Object.entries(chunks_to_load)) {
        if (!g_chunk_model.pending_requested_chunks.hasOwnProperty(chunk_key)) {
          g_chunk_model.pending_requested_chunks[chunk_key] = chunk_coords;
          chunks_coords_to_load.push(chunk_coords);
        }
      }
      this.sendChunkLoadRequest(chunks_coords_to_load);
      for (var i = 0; i < stale_chunks.length; i++) {
        delete g_chunk_model.available_chunks[stale_chunks[i]];
      }
      this.updateViewportFlags();
    }
    repopulateChunks() {
      var required_chunks = this.enumerateRequiredChunks();
      var loaded_required_chunks = {}; // Chunks that we need
      var unloaded_required_chunks = {}; // Chunks to request
      for (const [chunk_key, chunk_coords] of Object.entries(required_chunks)) {
        if (g_chunk_model.available_chunks.hasOwnProperty(chunk_key)) {
          loaded_required_chunks[chunk_key] = chunk_coords;
        } else {
          unloaded_required_chunks[chunk_key] = chunk_coords;
        }
      }
      var stale_chunks = []; // Chunks to evict from memory.
      for (var chunk_key in g_chunk_model.available_chunks) {
        if (!loaded_required_chunks.hasOwnProperty(chunk_key)) {
          stale_chunks.push(chunk_key);
        }
      }
      this.addOutputLine("Chunks to load: " + Object.keys(unloaded_required_chunks));
      this.addOutputLine("Chunks to evict: " + stale_chunks);
      this.loadChunksAndEvictStaleOnes(unloaded_required_chunks, stale_chunks);
    }
    onPositionChange(delta_x, delta_y, delta_z) {
      var new_camera_center_x = this.state.camera_center_x;
      var new_camera_center_z = this.state.camera_center_z;
      /*  Move camera around if the player tries to move out of bounds. */
      if (delta_x > 0 && this.state.x - this.state.camera_center_x > 10) {
        new_camera_center_x += BigInt(1);
      }
      if (delta_x < 0 && this.state.x - this.state.camera_center_x < -10) {
        new_camera_center_x -= BigInt(1);
      }
      if (delta_z > 0 && this.state.z - this.state.camera_center_z > 10) {
        new_camera_center_z += BigInt(1);
      }
      if (delta_z < 0 && this.state.z - this.state.camera_center_z < -10) {
        new_camera_center_z -= BigInt(1);
      }
      this.setState({
        x: delta_x + this.state.x,
        y: delta_y + this.state.y,
        z: delta_z + this.state.z,
        camera_center_x: new_camera_center_x,
        camera_center_z: new_camera_center_z
      }, () => this.repopulateChunks());
    }
    onKeyDown(e) {
      switch (e.keyCode) {
        case 65:
          {
            // A, Left
            this.onPositionChange(BigInt(-1), BigInt(0), BigInt(0));
            e.preventDefault();
            break;
          }
        case 87:
          {
            // W, Up
            this.onPositionChange(BigInt(0), BigInt(0), BigInt(1));
            e.preventDefault();
            break;
          }
        case 68:
          {
            // D, Right
            this.onPositionChange(BigInt(1), BigInt(0), BigInt(0));
            e.preventDefault();
            break;
          }
        case 83:
          {
            // S, Down
            this.onPositionChange(BigInt(0), BigInt(0), BigInt(-1));
            e.preventDefault();
            break;
          }
        case 32:
          {
            // Space
            this.onPositionChange(BigInt(0), BigInt(1), BigInt(0));
            e.preventDefault();
            break;
          }
        case 16:
          {
            // Down
            this.onPositionChange(BigInt(0), BigInt(-1), BigInt(0));
            e.preventDefault();
            break;
          }
        default:
          {}
      }
    }
    onClose(e) {
      this.addOutputLine("Connection Closed");
    }
    addOutputLine(new_line) {
      this.setState((prevState, props) => {
        let output_lines = prevState.output_lines.slice();
        output_lines.push(new_line);
        return {
          output_lines
        };
      });
    }
    onDescribeRegionsMessage(m) {
      this.addOutputLine("Got DescribeRegionsMessage: m.cuboids.length=" + m.cuboids.length + ", m.num_dimensions=" + m.num_dimensions);
      let lines = g_chunk_model.update_chunk_model_with_cuboids(m.cuboids);
      for (let i = 0; i < lines.length; i++) {
        this.addOutputLine(lines[i]);
      }
      this.updateViewportFlags();
    }
    onBinaryMessage(ab) {
      let dab = DynamicArrayBuffer.from_array_buffer(ab);
      let message_type_int64 = dab.read_one_int_64();
      switch (message_type_int64) {
        case BLOCK_MESSAGE_TYPE_DESCRIBE_REGIONS:
          {
            this.onDescribeRegionsMessage(DescribeRegionsMessage.from_byte_array(ab));
            break;
          }
        default:
          {
            console.log("Unknown message id=" + message_type_int64);
            break;
          }
      }
    }
    onConnect() {
      if (this.state.websocket === undefined || this.state.websocket.readyState === WebSocket.CLOSED) {
        this.addOutputLine("Trying to establish a WebSocket connection to '" + this.state.websocket_url + "'.");

        // Create a websocket
        var new_websocket = new WebSocket(this.state.websocket_url);
        new_websocket.binaryType = "arraybuffer";
        new_websocket.onopen = e => {
          this.addOutputLine("Connected!");
          this.repopulateChunks();
        };
        new_websocket.onmessage = e => {
          if (typeof e.data === 'string' || e.data instanceof String) {
            this.addOutputLine("Got string websocket message: " + e.data);
          } else {
            // Binary data
            this.onBinaryMessage(e.data);
          }
        };
        new_websocket.onclose = e => {
          this.onClose(e);
        };
        this.setState({
          websocket: new_websocket
        });
      } else {
        this.addOutputLine("Connection closed or undefined.");
      }
    }
    onSingleBlockWrite() {
      var text = this.state.input_value;
      let lower_coordinate = new Coordinate([this.state.x - BigInt(1), this.state.y, this.state.z - BigInt(1), BigInt(0)]);
      let upper_coordinate = new Coordinate([this.state.x + BigInt(1) + BigInt(1), this.state.y + BigInt(1), this.state.z + BigInt(1) + BigInt(1), BigInt(0) + BigInt(1)]);
      let array_of_data_to_send = [new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text), new TextEncoder("utf-8").encode(text)];
      let cuboid_addresses = new CuboidAddress(lower_coordinate, upper_coordinate);
      let cuboid_data_lengths = CuboidDataLengths.from_array_of_array_buffers(array_of_data_to_send);
      let cuboid_data = CuboidData.from_array_of_array_buffers(array_of_data_to_send);
      let cuboids_list = [new Cuboid(cuboid_addresses, cuboid_data_lengths, cuboid_data)];
      let write_regions_message = new DescribeRegionsMessage(cuboids_list, NUM_DIMENSIONS);
      var buffer_to_send = write_regions_message.as_array_buffer();
      console.log(new Uint8Array(buffer_to_send));
      this.state.websocket.send(buffer_to_send);
    }
    onInputValueChange(e) {
      this.setState({
        input_value: e.target.value
      });
    }
    render() {
      var output_items = [];
      for (var i = 0; i < this.state.output_lines.length; i++) {
        output_items.push( /*#__PURE__*/React.createElement("div", null, this.state.output_lines[i]));
      }
      var current_ws_state_names = {};
      current_ws_state_names[WebSocket.CONNECTING + ""] = "CONNECTING";
      current_ws_state_names[WebSocket.OPEN + ""] = "OPEN";
      current_ws_state_names[WebSocket.CLOSING + ""] = "CLOSING";
      current_ws_state_names[WebSocket.CLOSED + ""] = "CLOSED";
      var current_ws_state_colours = {};
      current_ws_state_colours[WebSocket.CONNECTING + ""] = "yellow";
      current_ws_state_colours[WebSocket.OPEN + ""] = "green";
      current_ws_state_colours[WebSocket.CLOSING + ""] = "gray";
      current_ws_state_colours[WebSocket.CLOSED + ""] = "brown";
      var current_ws_state = this.state.websocket === undefined ? "undefined" : current_ws_state_names[this.state.websocket.readyState + ""];
      var current_ws_colour = this.state.websocket === undefined ? "gray" : current_ws_state_colours[this.state.websocket.readyState + ""];
      return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
        style: {
          textAlign: "right"
        }
      }, /*#__PURE__*/React.createElement("button", {
        style: {
          fontSize: "200%",
          backgroundColor: current_ws_colour
        }
      }, this.state.websocket_url, ", State = ", current_ws_state)), /*#__PURE__*/React.createElement("h1", null, "Block Manager, x=", this.state.x.toString(), ", y=", this.state.y.toString(), ", z=", this.state.z.toString()), /*#__PURE__*/React.createElement(BlockGrid, {
        x: this.state.x,
        y: this.state.y,
        z: this.state.z,
        camera_outer_x_size: this.state.camera_outer_x_size,
        camera_outer_z_size: this.state.camera_outer_z_size,
        camera_center_x: this.state.camera_center_x,
        camera_center_z: this.state.camera_center_z,
        top_level_state: this.state
      }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("form", {
        "accept-charset": "utf-8"
      }, /*#__PURE__*/React.createElement("input", {
        type: "text",
        id: "input",
        onChange: e => this.onInputValueChange(e),
        value: this.state.input_value
      }))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("input", {
        type: "button",
        value: "SEND",
        onClick: () => this.onSingleBlockWrite()
      })), /*#__PURE__*/React.createElement("div", null, output_items));
    }
  }
  /*
  Each call uses a list of regular cuboids:
  	SubscribeRegions  3  0,0,0 1,1,1; 8,8.8 12,12,12; 3,3,3 3,3,3
  ReadRegions  2  0,0,0 1,1,1; 8,8.8 12,12,12  ->  Response 0,0,0 1,1,1, (cuboid co-ordinates) followed by the blocks data <>
  	DescribeRegions 1  0,0,0 1,1,1 <len of 0,0,0> <data of 0,0,0>, <len of 0,0,1> <data of 0,0,1>, ..., <len of 1,1,1> <data of 1,1,1>
  	-  Order does not matter.  For block data, blocks are sent/received in order of lowest x, then y, then z first.
  -  co-ordintates are diagonal endpoints contained in the cuboid under consideration.
  -  Non-existant block is different than 0 length block.  ReadRegions may return 0 results, or contain 'gaps' for non-existant blocks.
  	*/
  function App() {
    return /*#__PURE__*/React.createElement(HomePageUI, null);
  }
  const rootElement = document.getElementById('root');
  ReactDOM.render( /*#__PURE__*/React.createElement(App, null), rootElement);
});
