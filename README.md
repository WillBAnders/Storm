# Storm

Storm is a library for user-oriented configuration. Using Storm, developers can
easily create type-safe systems for working with text-based configurations,
especially for non-technical users.

 - [Discord](https://discord.gg/xdVZzSN): Development discussion & support

## Features

 - Navigable node hierarchy with no implicit behavior
 - Type-safe serialization system including object mapping
 - Custom configuration format and online editor support
 - Extensions for JSON, HOCON, YAML, and XML
 - Well documented, specified, and SemVer compatible APIs
 - Complete test coverage over the API and implementation

## Motivation

The majority of design decisions in Storm are based on experiences working with
[HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) as well as
non-technical users. The following are some of the primary issues that caused
confusion for users (and developers), many of which can be avoided.

 - Only `\n` is allowed for newlines. Files encoded with `\r` (MacOS) often
 result in cryptic error messages which is difficult to debug unless known.
 - The top-level value must be an object (`{...}`), which means HOCON is not
 strictly compatible with JSON though it intends to be a JSON superset.
 - The order of object properties is not maintained, thus re-serialization often
 scrambles the config making it difficult to keep a logical ordering.
 - Values are coerced between types, such as:
    - `"true"`, `"on"`, and `"yes"` all becoming the boolean `true`
    - `3.5` and `"3"` becoming the integer `3` and the doubles `3.5` and `3.0`
    - Objects containing *any* numeric key coercing to a list, excluding
    non-numeric keys entirely.

## Development Progress

Development is currently focused on finalizing the serialization system,
particularly with edge cases *that just keep getting in the way :/*.

If you're interested in contributing, feel free to join the project
[Discord](https://discord.gg/xdVZzSN).
