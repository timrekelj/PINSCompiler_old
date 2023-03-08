# PINS Compiler

### How to run compiler without tests
1. create file with code in `/<PINS root>/<your_file_name>` (`quickTest.pins` is already created for you but you can create new one)
2. Run from root (with args `PINS <your_file_name> --dump <phase>`)

| Valid phase names are `LEX`, `SYN`, `AST`, `NAME`, `TYP`, `FRM`, `IMC`, `INT`

### How to use tests
1. Create file with code (`<your_test_file>.pins`) and expected output (`<your_output_file>.out`)
1. Add both files to `/<PINS root>/src/tests/<your_folder_name>/`
2. Run from root (with args `TEST --folder <your_folder_name>`) and only the tests from `<your_folder_name>` will be used

### `launch.json` example for VS Code (running only lexer with tests in LEX folder)
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Test Compiler",
            "request": "launch",
            "mainClass": "tests.Test",
            "args": "TEST --folder LEX"
        },
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