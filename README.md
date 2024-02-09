# Kafcat

Small utility to subscribe to kafka topic and print events to stdout

# Usage

```
Usage: kafcat [--abort] [--quiet] [--broker <string>] [--groupid <string>] [--registry <string>] [--key-deserializer <Deserializer>] [--value-deserializer <Deserializer>] [--format <string>] <topic>

Consume events from a Kafka topic and print them to stdout

Options and flags:
    --help
        Display this help text.
    --version, -v
        Print the version number and exit.
    --abort, -a
        Abort on failure
    --quiet, -q
        Do not output failures to stderr
    --broker <string>, -b <string>
        Broker address and port
    --groupid <string>, -g <string>
        Consumer Group ID
    --registry <string>, -r <string>
        Registry URL
    --key-deserializer <Deserializer>, -k <Deserializer>
        Key deserializer. Default is string. One of:
         * string
         * long
         * avro
         * raw
    --value-deserializer <Deserializer>, -v <Deserializer>
        Value deserializer. Default is string. One of:
         * string
         * long
         * avro
         * raw
    --format <string>, -f <string>
        Output format with templating. Default is "%k => %v". Valid template variables are:
         * %k Key
         * %v Value
         * %t Topic name
         * %p Partition
         * %o Offset
         * %d Timestamp
         * %h Headers
    --number <integer>, -n <integer>
         Take N records and quit
```
