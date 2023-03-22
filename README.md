# PINS Compiler

### How to run specific phases of compiler
1. create file with code in `/<PINS root>/<your_file_name>` (`quickTest.pins` is already created for you but you can create new one)
2. Run from root (with args `PINS <your_file_name> --dump <phase>`)

| Valid phase names are `LEX`, `SYN`, `AST`, `NAME`, `TYP`, `FRM`, `IMC`, `INT`

### `launch.json` example for VS Code (running only lexer with tests in LEX folder)
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch LEX",
            "request": "launch",
            "mainClass": "Main",
            "args": "PINS quickTest.pins23 --dump LEX"
        }
    ]
}
```