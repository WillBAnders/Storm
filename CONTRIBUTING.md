# Contributing

Thank you for your interest in contributing! Before making a contribution,
please review the following guidelines.

 - Prior to starting code contributions, please discuss the changes in an issue,
   on [Discord](https://discord.gg/xdVZzSN), or with a maintainer. This is to
   ensure we're aware what is being worked on to help us administrate the
   project effectively.
 - Please keep PRs to a single topic. Merging is all or nothing, so PRs with
   multiple changes will take longer and are less likely to be merged.
 - PRs that introduce breaking changes are generally discouraged as these only
   occur with major versions and tend to spawn from internal discussion.
   Instead, please open an issue to discuss breaking changes first.
 - PRs that are 'cosmetic' in nature, such as reformatting or fixing typos, are
   not accepted. See [this explanation](https://github.com/rails/rails/pull/13771#issuecomment-32746700)
   on the rails repository for why these can be detrimental to a project.
   Instead, please bring it to the attention of a maintainer.

If you don't know where to start, please reach out to a maintainer and we'd be
happy to connect you to ways you can help contribute.

## Git Workflow

Development is focused around the `master` branch, which contains the latest
changes for the next release. The following branch structures are also used:

 - `release/vX`: Releases by major version `X`
 - `fix/name`: Fixes for issues, generally for PRs
 - `feature/name`: New features, generally for PRs
 - `refactor/name`: Code refactors/rewrites, generally internal

For maintainers, any changes the affect the API should be handled through PRs to
allow time for feedback and review on the changes. This is **necessary** for
breaking changes and **optional** for non-breaking changes - use judgement.

## Code Style

We use [Google's Java Style Guide](https://google.github.io/styleguide/javaguide.html)
with the following modifications/clarifications.

 - License Information ([3.1](https://google.github.io/styleguide/javaguide.html#s3.1-copyright-statement))
    - A license header is not included in individual source files.
 - Import Order ([3.3.3](https://google.github.io/styleguide/javaguide.html#s3.3.3-import-ordering-and-spacing))
    - Place java/javax imports in a separate block below other imports.
 - Static Imports ([3.3.4](https://google.github.io/styleguide/javaguide.html#s3.3.4-import-class-not-static))
    - Do not use static imports.
 - Empty Blocks ([4.1.3](https://google.github.io/styleguide/javaguide.html#s4.1.3-braces-empty-blocks))
    - Braces for empty blocks are on the same line.
 - Block Indentation ([4.2](https://google.github.io/styleguide/javaguide.html#s4.2-block-indentation))
    - Use `4` spaces for blocks.
 - Line Length ([4.4](https://google.github.io/styleguide/javaguide.html#s4.4-column-limit))
    - Code lines should not exceed `120` characters.
    - Javadocs should not exceed `80` characters.
 - Line Wrapping ([4.5](https://google.github.io/styleguide/javaguide.html#s4.5-line-wrapping))
    - Use `8` spaces for continuation (as normal indentation is `4` spaces).
 - Vertical Spacing ([4.6.1](https://google.github.io/styleguide/javaguide.html#s4.6.1-vertical-whitespace))
    - Include blank lines at the start and end of a class declaration.
 - Horizontal Alignment ([4.6.3](https://google.github.io/styleguide/javaguide.html#s4.6.3-horizontal-alignment))
    - Do not align variables (or anything else) in this manner.
 - Annotations ([4.8.5](https://google.github.io/styleguide/javaguide.html#s4.8.5-annotations))
    - Method annotations are defined on separate lines.
    - Field annotations are defined on the same line.
 - Numeric Literals ([4.8.8](https://google.github.io/styleguide/javaguide.html#s4.8.8-numeric-literals))
    - Use uppercase letters for type suffixes and hexadecimal characters.
    - Use lowercase letters for bases and exponents (`0xFF`, `1e9`).
 - Unused Exceptions ([6.2](https://google.github.io/styleguide/javaguide.html#s6.2-caught-exceptions))
    - Exceptions which are unused should be named `ignored`.
