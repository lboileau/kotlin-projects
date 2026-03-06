# Common Library — AI Context

## Overview
Shared utilities and helpers with no I/O dependencies.

## Package
`com.acme.libs.common`

## Public API
- `logger()`: Inline reified function that creates an SLF4J `Logger` for the calling class via `LoggerFactory.getLogger(T::class.java)`

## Gradle Module
`:libs:common`
