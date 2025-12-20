# MKDocs

MKDocs is the program used to take all of the markdown documentation files and
compile them into a static HTML site. The MKDocs docs can be found
[here](https://www.mkdocs.org/).

You should install MKDocs if you want to debug the site.

## Installation

To install MKDocs, see MKDocs' [installation
page](https://www.mkdocs.org/user-guide/installation/).

If you are using Linux, you should ignore the above advice and just use your
distribution's package manager.

In order to install plugins, you can use the same installation method as above.
For example, to install `mkdocs-material`, I would use the following:

```
pip install mkdocs-material
```

## Hacking

You can hack on the MKDocs build by adjusting the settings in
[mkdocs.yml](https://github.com/Team2122/2122_TeamTators/blob/main/documentation/mkdocs.yml).
This file contains all of the settings MKDocs considers when compiling the site,
such as the theme and plugins.

One of the plugins used is called `mkdocs-material`. This provides both a nice
looking theme and lots of easy to set up plugins. They have *amazing*
documentation [here](https://squidfunk.github.io/mkdocs-material/)

With regards to running MKDocs, there are three important subcommands to know:

- `serve`
- `build`
- `gh-deploy`

Each of these are called as `mkdocs <command>` in a terminal that has `mkdocs`
installed. For example, to build the documentation, I would enter `mkdocs build`
into my terminal.

Each of these are expanded upon more [below](#common-subcommands)

## Common Subcommands

### Serve

The command `mkdocs serve` will both build the site and start a webserver on
your machine you can connect to at `localhost:8000` in your browser. This should
let you preview the site as it will be when it is published.

The webpage in your browser will automatically update as you make changes to the
documentation. You do not have restart this command while iterating on
documentation.

### Build

You can use `mkdocs build` to compile the documentation into its final form. By
default, the site is output into a directory called `site`.

### GH Deploy

All `mkdocs gh-deploy` does is build the site and push the contents of the site
to the `gh-pages` branch. It is very unlikely you will need to invoke this
manually, and documented exclusively because it is used in the CI job that runs
on every merge to main.
