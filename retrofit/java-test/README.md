# Retrofit Java Tests

These are in a separate module for two reasons:

- It ensures optional dependencies (Kotlin stuff) are completely absent.
- It uses the multi-release jar on the classpath rather than only the classes folder.
