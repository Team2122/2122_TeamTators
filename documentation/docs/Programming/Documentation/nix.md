# Nix

Nix is a build tool and package repository that can be used to automatically
create a development shell with all of the required tools inside of it:
primarily, MKDocs and the plugins we use.

The purpose of Nix is to provide a reproducible environment such that running
`nix develop` (the command to enter the provided development shell) will give
you the exact same shell with the exact same packages, all on the same versions
as if you ran it 5 years ago or 5 years in the future.

Nix is primarily used to power the CI for deploying the website.

Nix **is not required** when debugging locally, and is **only natively supported
on Linux and Mac** (although you can use it from within WSL on Windows).

## Why Nix?

The point of using Nix in the CI is primarily because the plugin used to render
`.excalidraw` files for MKDocs has an absolutely atrocious build system that
involves building both the python plugin and the javascript rendering code. The
original architect of this documentation repository also happened to get this
building through Nix due to their personal workflow involving the tool, and
replicating this in a GitHub actions runner without nix was a somewhat involved
task, so Nix it was.

Additionally, Nix provides a very convenient way to create reproducible shells
to prevent future breakages in the CI.

## How Nix?

Before beginning, I should note that Nix is a complicated language/tool/package
repository. This document is aimed at describing what our Nix flake specifically
does, not at explaining Nix as a whole. For more information on the nix language
in general, see [this
introduction](https://nixos.org/guides/nix-pills/04-basics-of-language) and
[followup on functions and
imports](https://nixos.org/guides/nix-pills/05-functions-and-imports.html). More
information on flakes specifically can be found
[here](https://vtimofeenko.com/posts/practical-nix-flake-anatomy-a-guided-tour-of-flake.nix/).

The first thing to understand about Nix is that it is first and foremost a
package manager, à la the Microsoft Store or Steam. However, Instead of clicking
buttons to manage packages, packages are defined through a programming language
also called Nix, and those packages can be used as the outputs of a **Nix flake**.

The flake for this repository can be found in [a file at the project root
called
`flake.nix`](https://github.com/Team2122/2122_TeamTators/blob/main/documentation/flake.nix).
Inside of this flake, you will find two primary sections: `inputs` and
`outputs`. In Nix, the inputs of your flake are the dependencies of it, such as
nixpkgs, the Nix package repository, among others. Your outputs are what you
can actually use, such as packages or development shells.

We define several outputs. Namely, at the time of writing, the `beautifulsoup4`,
`excalidraw-renderer`, and `mkdocs-excalidraw` packages, as well as a `devShell`
output. These can be visualized through the `nix flake show` command:

```
├───devShells
│   ├───aarch64-darwin
│   │   └───default omitted (use '--all-systems' to show)
│   ├───aarch64-linux
│   │   └───default omitted (use '--all-systems' to show)
│   ├───x86_64-darwin
│   │   └───default omitted (use '--all-systems' to show)
│   └───x86_64-linux
│       └───default: development environment 'nix-shell'
└───packages
    ├───aarch64-darwin
    │   ├───beautifulsoup4 omitted (use '--all-systems' to show)
    │   ├───excalidraw-renderer omitted (use '--all-systems' to show)
    │   └───mkdocs-excalidraw omitted (use '--all-systems' to show)
    ├───aarch64-linux
    │   ├───beautifulsoup4 omitted (use '--all-systems' to show)
    │   ├───excalidraw-renderer omitted (use '--all-systems' to show)
    │   └───mkdocs-excalidraw omitted (use '--all-systems' to show)
    ├───x86_64-darwin
    │   ├───beautifulsoup4 omitted (use '--all-systems' to show)
    │   ├───excalidraw-renderer omitted (use '--all-systems' to show)
    │   └───mkdocs-excalidraw omitted (use '--all-systems' to show)
    └───x86_64-linux
        ├───beautifulsoup4: package 'python3.13-beautifulsoup4-4.13.5'
        ├───excalidraw-renderer: package 'excalidraw-renderer-0.6.0'
        └───mkdocs-excalidraw: package 'mkdocs-excalidraw-0.6.0'
```

Each of these outputs can be found in `flake.nix`. For packages, you can search
for `packages.<package-name>`. The development shell is defined at the bottom of
the flake in `devShells.default`, with its packages defined in `buildInputs`.

At time of writing, the default `devShell` includes the following packages:

- `mkdocs`
- `python313Packages.mkdocs-material`
- `self'.packages.mkdocs-excalidraw`

`self'.packages.mkdocs-excalidraw` refers to the package defined in our flake
that builds the mkdocs-excalidraw plugin. This package builds the python code
MKDocs uses to actually do the importing of excalidraw files. The
`mkdocs-excalidraw` package we define is dependent on both the
`excalidraw-renderer` and `beautifulsoup4` packages we define.

`excalidraw-renderer` builds the javascript code that the `mkdocs-excalidraw`
python code calls into in order to actually render a `.excalidraw` file as a
png. 

`beautifulsoup4` is actually already packaged in nixpkgs, but the version that's
packaged is too old for `mkdocs-excalidraw`, so all we do is tell Nix to use the
definition already in nixpkgs but fetch & build a newer version.
