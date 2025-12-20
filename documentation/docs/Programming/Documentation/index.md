# The Documentation Documentation

This document is aimed at describing the process by which this documentation
site is built and published should it need be maintained in the future.

The publishing sequence is pretty simple and goes as follows:

1. A change to documentation is merged to main
2. Github runs a CI job to run `mkdocs gh-deploy`, which builds the markdown
   files into an HTML website and pushes the site to the `gh-pages` branch
3. Github sees an update on the `gh-pages` branch and publishes the contents of
   it to
[https://Team2122.github.io/2122_TeamTators](https://Team2122.github.io/2122_TeamTators)
   
How these are run will be explained in more detail in other pages.
