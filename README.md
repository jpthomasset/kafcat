![Build](https://github.com/jpthomasset/kafcat/actions/workflows/scala.yml/badge.svg) - ![Docker image](https://img.shields.io/docker/v/jpthomasset/kafcat)

# Kafcat

Small utility to subscribe to kafka topic and print events to stdout.

# Usage

You can either run locally or from a docker image:

```
docker run --rm docker.io/jpthomasset/kafcat:latest -b broker-url:9092 my-topic
```

## Command line options

```
Usage: kafcat [--abort] [--quiet] [--broker <url>] [--registry <url>] [--key-deserializer <Deserializer>] [--value-deserializer <Deserializer>] [--format <string>] [--predicate <string>] [--number <N>] [--skip <N>] [--skip-null] [--timeout <N>] [--offset-reset <strategy>] [--since <iso-date-time>] <topic>

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
    --broker <url>, -b <url>
        Broker address and port
    --registry <url>, -r <url>
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
    --predicate <string>, -p <string>
        Predicate to filter records. You can use the following operators: ==, !=, ||, &&
         Then you can use value/key field names and constants:
         * value.field to extract a field from the value of the event
         * key.field to extract a field from the key of the event
         * topic
         * partition
         * offset
         * "some string" to use a string constant
         * 123.45 to use a number constant
        Here are some examples:
         * "value.id == 12"
         * "key.id == 12"
         * "value.sub.subage == 15"
         * "value.sub.subname == 'subname' || key.id == 12"
         * "topic == 'some topic' && value.id == 12"
    --number <N>, -n <N>
        Take N records and quit
    --skip <N>, -s <N>
        Skip N records and quit
    --skip-null
        Skip records with null values
    --timeout <N>
        Timeout after N seconds
    --offset-reset <strategy>
        Offset reset strategy. One of earliest or latest. Default to latest
    --since <iso-date-time>
        Start consuming from this timestamp (ISO Format)
```
