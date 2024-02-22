# Bookie

This is a text editor with extended Markdown support and website generation features:
- custom Markdown features
- tabbed file editor
- compiled file previews
- project export for local viewing and Flask

Files defined using the extension .bd can be processed using the Bookie compiler, otherwise they are handled using the base flavour (see Extended Syntax).

## Extended Syntax

The custom features are not implemented as a full Markdown flavour, rather they are dependent on a base flavour (Github Markdown Flavour by default).
Wherever a file is referenced, the custom features will only work properly if the file can be resolved at the time of compilation, otherwise, an error message will display on the page, or the base flavour will take over depending on the block type.
File paths may be relative, though you cannot reference files outside of the project directory.

### Video support

Image links can now be used for referencing videos both locally and at remote addresses.
The syntax for this is identical to standard Markdown, but the resulting HTML will be a \<video> if the referenced file can be resolved as a video.

> For example:
> ```markdown
> ![Video](path/to/video)
> ```
> becomes:
> ```html
> <video src="path/to/video" alt="Video" />
> ```

### Figures

Images and videos can be converted into figures which may be referenced on that page.
To do this, a figure reference tag needs to be added to the start of the alt description for an image/video.

> Defining a figure:
> ```markdown
> ![pie: A lovely picture of a pie](path/to/pie.png)
> ```
> becomes (where x is the figure index):
> ```html
> <figure>
>   <img id="pie" src="path/to/pie.png" alt="A lovely picture of a pie" />
>   <figcaption>Figure x: A lovely picture of a pie</figcaption>
> </figure>
> ```

The figure defined here may be referenced before and after it's position on the page using the reference tag.

> Referencing a figure:
> ```markdown
> In the world of pies, the Pie Pie reigns supreme, as shown in Figure {pie}.
> ```
> becomes (where x is the figure index):
> ```html
> <p>In the world of pies, the Pie Pie reigns supreme, as shown in Figure <a href="#pie">x</a>.</p>
> ```

Figures may be referenced in paragraphs where the following node types are the only ones present (so as to maintain base flavour compatibility): 
text, white space, breaks, EOL, left and right brackets, short reference link, inline reference link and full reference link.

### Enhanced code blocks

Code blocks will now be converted into [Ace Editor](https://github.com/ajaxorg/ace) objects automatically, enabling editing, syntax highlighting and code completion based on the specified language.
Additionally, code blocks can have their contents reset to the original value with a button placed nearby*.

> Creating a code block using a code fence:
> ~~~markdown
>   ```python
>   print("Hello, world!")
>   ```
> ~~~
> becomes (where x is the index of the code block):
> ```html
> <div id="codeblock_x">
>   print("Hello, world!")
> </div>
> ```

Once the page has loaded, the Ace library will convert that div to an Ace editor setup to use the language specified for code completion and syntax highlighting.
Additionally, code blocks may be derived from file links in a code span if the file can be resolved at compile time.

> Creating a code block from a span:
> ```markdown
> `language from path/to/file.lang`
> ```

**\* The reset functionality will only work locally if the code block contents are defined in the .bd file due to issues with CORS.**

### Code running (requires RemoteCodeRunner)

write about this

### Enhanced headings (tbc)

I may add support for on-page contents generation based on headings levels, but we shall see.

## Editor features

The Bookie Editor (forgive the currently not-so-pretty UI) has the following features:
- tabbed editing - multiple files may be open at once and be edited by switching tabs
- file navigation - a file tree can be found on the left side, with drag and drop support into directories
- creating a new project will provide a basic directory structure for getting started
- the open project will persist until it is closed by the user

Handling of non-text files is delegated to the operating system, so attempting to open an image, for example, would use the default system application for images.

## Compilation

As previously mentioned, Bookie supports three types of file compilation: preview, local export and Flask export.
Building a file will render the custom Markdown syntax and collect any local resources, then place them relative to the output .html file.
Each of these is similar, but has certain differences in the HTML and file structure produced.

### Preview

The editor allows right-clicking on a file and pressing "Open in browser" to build the file and preview it, or selecting a file and using the shortcut.
The title will indicate that it is a preview and there will be no cross-page navigation, but the main content will be identical to other methods.
The results of compilation are stored in the "out" directory of a given project.
The Ace Editor code will be added to the top level directory, and all files using code blocks will use a relative definition to this.

### Contents page (for local and Flask export)

Exporting both locally and for Flask depend on a contents page to define the contents of the home page and the chapters to compile and their ordering.
The contents page is treated specially, as it permits "chapter definitions", which refers to this syntax: `name (path/to/chapter)` to define chapters for compilation.
Chapter definitions are not recognised from within chapters, only in the contents page.
This functionality should be defined in the top-level project directory in a file called "front_matter.bd", and a special context menu shortcut exists to regenerate this file.

### Local export (recommended for local testing and manual web server creation)

Exporting locally will start by compiling the contents page and reading the chapter files to compile.
In the order of their definition, chapters will be compiled with a title derived from the chapter definition and its index in the contents file, the main content and navigation at the bottom of the page for previous, next and the contents page.
Static resources such as images or code files will be stored in the same structure as they were in the project, so a file at root/Images/more/img.png would be exported to exported/Images/more/img.png.
The Ace Editor code will be added to the top level directory, and all files using code blocks will use a relative definition to this.

### Flask export (recommended for automatic web server creation - requires Python 3)

Exporting for Flask means that the resulting file structure will conform to the expectations of [Flask](https://github.com/pallets/flask), and the appropriate support files will be generated.
Static resources are moved into a folder called "static", and .bd files will compile into the "templates" folder, both with the same file structure as the project.
Based on the chapter structure, methods will be created for each page with the appropriate routing in "app.py".
Scripts for installing and running the server are packaged with the Flask app (install.sh and start.sh, as well as their Windows counterparts) and can be used to simplify setting up the server after it has been created.
Additionally, a file called "bookie.properties" will be added to the directory, with one entry for the code server URL (http://localhost:8080 by default), but this can be modified as needed, and entries can be added if desired for custom functions.
This export mode is the only one which supports running code due to CORS restrictions.

**Note: the script files need to be run from the root directory of the Flask app, ideally through a terminal. If they do not run, it is likely they do not have execute permissions. In this case,
please run `chmod +x install.sh start.sh` in the terminal for macOS and Linux, or set the execute permissions for each file in "Properties > Security > Allow executing file as program" for Windows.**
