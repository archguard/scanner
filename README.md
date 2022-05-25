# MOVE TO [https://github.com/archguard/archguard](https://github.com/archguard/archguard)

# Arch Scanner

[![CI](https://github.com/archguard/scanner/actions/workflows/ci.yaml/badge.svg)](https://github.com/archguard/scanner/actions/workflows/ci.yaml)
[![codecov](https://codecov.io/gh/archguard/scanner/branch/master/graph/badge.svg?token=RSAOWTRFMT)](https://codecov.io/gh/archguard/scanner)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/archguard/scanner)

Requirements: JDK 12

Scanner:

* diff_change - diff change between commits
* scan_git - Git commit history scan
* analyser_architecture - analysis architecture
* analyser_sca - analysis software composition
* scan_sourcecode - Code analysis
  * scanner_sourcecode
       * feat_apicalls
       * feat_datamap
       * lang_csharp
       * lang_golang
       * lang_java
       * lang_kotlin
       * lang_python
       * lang_scala
       * lang_typescript
  * scanner_cli
  * scanner_core
* linter
  * [x] rule_sql
  * [x] rule_test_code
  * [x] rule_webapi
  * [ ] rule_layer
* rule-core

```mermaid
flowchart TB
    subgraph ast
    lang_java-->AST
    lang_kotlin-->AST
    lang_typescript-->AST
    lang_csharp-->AST
    lang_python-->AST
    lang_others[...]-->AST
    end

    AST --> Backend

    AST --> feat_datamap --> Backend
    AST --> feat_apicalls --> Backend

    
    subgraph deps
    gradle --> sca
    maven --> sca
    npm --> sca
    others[...] --> sca
    end

    sca --> Backend

    subgraph Linter
    AST --> rule_sql --> Issue
    AST --> rule_test_code --> Issue
    AST --> rule_webapi --> Issue
    end

    Issue --> Backend

    scan_git --> Backend
    diff_changes --> Backend

    feat_apicalls --> arch

    subgraph archteicture
    AST --> arch
    sca --> arch
    code --> arch
    end

    archteicture --> Backend
```

## Inspires

ArchGuard Scanner is inspired by a lot of projects.

- scan_bytecode inspired by [https://github.com/fesh0r/fernflower](https://github.com/fesh0r/fernflower)
- linter rule system inspired by [https://github.com/pinterest/ktlint](https://github.com/pinterest/ktlint)
- CLOC inspired by [https://github.com/boyter/scc](https://github.com/boyter/scc), and `languages.json` based on SCC with MIT LICENSE.

License
---

`languages.json` based on [https://github.com/boyter/scc](https://github.com/boyter/scc) with MIT LICENSE.

@2020~2022 Thoughtworks. This code is distributed under the MIT license. See `LICENSE` in this directory.
