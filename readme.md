# Socomo - care about the composition of your java code 

Socomo is a simple tool to visualize the composition of your source code and track changes to the
composition as you progress with the development of your java project. You can use this tool to:

- Explain internal design of your modules to newcomers by showing them the diagram of component dependencies.
- Assure that architecture of your project is not being broken by seeing what new dependencies are added.
- Brag on your blog or twitter how great the structure of your code is, proving it with the diagrams.

## Quick Start

Try Socomo now on your code:

```bash
mvn pl.gdela:socomo-maven:display
```

This will generate the `socomo.html` file and open it in the browser, where you'll see
the visualization of code structure. Here's an example for Guava project:

<p align="center">
  <img src="example.gif" alt="Composition of Guava viewed in Socomo">
</p>

To track changes to the composition commit the `socomo.html` file to your source code repository
and add [socomo-maven plugin](#maven-plugin) to your `pom.xml`. Whenever composition changes
the build plugin will update this human-readable file, so you'll have a history of changes
in the most convenient place - in your source code repository.

## How Does It Work

Socomo analyses bytecode to find dependencies between code members, so code needs to be compiled
first. The generated `socomo.html` file is a concise representation of code composition at chosen
**level** - the root package of the diagram. Each child package in the root, together with subpackages
contained in it, is considered a **component** and depicted on the diagram as a node. Edges between
nodes represent dependencies.

## Maven Plugin

Add this to the `<build><plugins>` section in your `pom.xml` file. For multi-module projects,
you can add it just to the parent pom, and it will be inherited by all modules:

```xml
<plugin>
  <groupId>pl.gdela</groupId>
  <artifactId>socomo-maven</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>analyze</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## Contributing

Raise an [issue or enhancement request](https://github.com/gdela/socomo/issues),
or better yet a [pull request](https://github.com/gdela/socomo/pulls).
Contact me at [wojciech@gdela.pl]() or [@WojciechGdela](https://twitter.com/WojciechGdela).
