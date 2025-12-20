# Include Directory

This directory is meant for files to be included using the `include` macro.

For example, if a file `foo.mmd` exists in this directory, and you want to embed
that file in a markdown document, you can use the following syntax:

`include/foo.mmd`:

```
pie title NETFLIX
         "Time spent looking for movie" : 90
         "Time spent watching it" : 10
```

`docs/foo.md`:

<pre>
Look at this cool graph!

``` mermaid
{% include 'foo.mmd' %}
```
</pre>
