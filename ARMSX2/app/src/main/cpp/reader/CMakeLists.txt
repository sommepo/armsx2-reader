# SPDX-FileCopyrightText: ARMSX2 Reader fork contributors
# SPDX-License-Identifier: GPL-3.0+

# The reader fork's own native code. The emulator's build includes this folder once.
#
#   reader_lua    Lua 5.4 (reader/lua), built as C++: its errors are then exceptions, which
#                 the bindings rely on to unwind.
#   reader_core   The text engine: profiles, text events. Knows nothing of the emulator.
#   reader/core/emulator.cpp ties the engine to the emulator and is built as part of its
#   core; reader/reader_jni.cpp is the bridge to the app's Kotlin side.
#
# The emulator is built without C++ exceptions; these two libraries are built with them, and
# nothing of theirs throws into the emulator's code.

set(READER_LUA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/lua)
set(READER_LUA_SOURCES)
foreach(name lapi lauxlib lbaselib lcode lcorolib lctype ldebug ldo ldump lfunc lgc llex
		lmathlib lmem lobject lopcodes lparser lstate lstring lstrlib ltable ltablib ltm
		lundump lutf8lib lvm lzio)
	list(APPEND READER_LUA_SOURCES ${READER_LUA_DIR}/${name}.c)
endforeach()
set_source_files_properties(${READER_LUA_SOURCES} PROPERTIES LANGUAGE CXX)
add_library(reader_lua STATIC ${READER_LUA_SOURCES})
target_include_directories(reader_lua SYSTEM PUBLIC ${READER_LUA_DIR})
target_compile_options(reader_lua PRIVATE -fexceptions -w)
set_target_properties(reader_lua PROPERTIES CXX_STANDARD 20 POSITION_INDEPENDENT_CODE ON)

add_library(reader_core STATIC
	core/audio_tap.cpp
	core/audio_tap.h
	core/crt.cpp
	core/crt.h
	core/engine.cpp
	core/engine.h
	core/input_filter.cpp
	core/input_filter.h
	core/next_choice.cpp
	core/next_choice.h
	core/lua_api.cpp
	core/lua_api.h
	core/text_codec.cpp
	core/text_codec.h
	core/types.h
)
target_include_directories(reader_core PUBLIC ${CMAKE_SOURCE_DIR})
target_link_libraries(reader_core PUBLIC reader_lua)
# The CRT picture is on in an install that was never told otherwise (reader/core/crt.cpp):
# this console was played on a tube.
target_compile_definitions(reader_core PRIVATE READER_CRT_DEFAULT_ON)
target_compile_options(reader_core PRIVATE -fexceptions)
set_target_properties(reader_core PROPERTIES CXX_STANDARD 20 CXX_STANDARD_REQUIRED ON
	POSITION_INDEPENDENT_CODE ON)

# The emulator's side.
target_sources(PCSX2 PRIVATE
	${CMAKE_CURRENT_SOURCE_DIR}/core/emulator.cpp
	${CMAKE_CURRENT_SOURCE_DIR}/core/emulator.h
	# The CRT picture as one of the emulator's present shaders.
	${CMAKE_CURRENT_SOURCE_DIR}/render/crt_present.cpp
	${CMAKE_CURRENT_SOURCE_DIR}/render/crt_present.h)
target_include_directories(PCSX2 PRIVATE ${CMAKE_SOURCE_DIR})
target_link_libraries(PCSX2 PUBLIC reader_core)
