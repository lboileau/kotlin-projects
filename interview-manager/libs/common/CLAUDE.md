# Common Library — AI Context

## Overview
Shared utilities, types, and helpers with no I/O dependencies.

## Package
`com.acmo.libs.common`

## Public API
- `logger()` — Reified inline extension function that creates an SLF4J Logger for any class via `LoggerFactory.getLogger(T::class.java)`

## Gradle Module
`:libs:common`
