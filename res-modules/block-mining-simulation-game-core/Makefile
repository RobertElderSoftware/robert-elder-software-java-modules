CC=g++
$(info "MVN_JAVA_HOME=${MVN_JAVA_HOME}")
READLINK_MVN_JAVA_HOME=$(shell readlink -f "${MVN_JAVA_HOME}")
$(info "READLINK_MVN_JAVA_HOME=${READLINK_MVN_JAVA_HOME}")

JAVA_HOME=$(READLINK_MVN_JAVA_HOME)
CFLAGS=-c -std=c++11 -Wall -fPIC -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
LDFLAGS=-fPIC -shared

SOURCES_DIR=src/main/c++
OBJECTS_DIR=target/c++
EXECUTABLE=target/classes/liblinux_block_jni.so

SOURCES=$(shell find '$(SOURCES_DIR)' -type f -name '*.cpp')
OBJECTS=$(SOURCES:$(SOURCES_DIR)/%.cpp=$(OBJECTS_DIR)/%.o)

all: $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	mkdir -p $(shell dirname $@)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

$(OBJECTS): $(SOURCES)
	mkdir -p $(OBJECTS_DIR)
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -rf $(OBJECTS_DIR) $(EXECUTABLE)
