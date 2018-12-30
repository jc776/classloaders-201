The [https://nrepl.org/nrepl/usage/misc.html|nREPL docs] say that "hot-loading" dependencies with e.g. pomegranate should work in `clj`.

In `lein`, this doesn't immediately work:
```
PS C:\Users\Jack\SSD\Projects\classloaders-201> lein run -m nrepl.cmdline --interactive
nREPL server started on port 53159 on host 127.0.0.1 - nrepl://127.0.0.1:53159
nREPL 0.5.3
Clojure 1.10.0
Java HotSpot(TM) 64-Bit Server VM 10.0.1+10
Interrupt: Control+C
Exit:      Control+D or (exit) or (quit)
user=> (require 'bidi.bidi)
Execution error (FileNotFoundException) at user/eval1282 (form-init7649922705840423474.clj:1).
Could not locate bidi/bidi__init.class, bidi/bidi.clj or bidi/bidi.cljc on classpath.
user=> (require '[cemerick.pomegranate :as pg])
nil
user=> (pg/add-dependencies :coordinates '[[bidi "2.1.5"]]
                     :repositories (merge cemerick.pomegranate.aether/maven-central {"clojars" "https://clojars.org/repo"}))
{[bidi "2.1.5"] #{[prismatic/schema "1.1.7" :exclusions [[org.clojure/tools.reader]]]}, [prismatic/schema "1.1.7" :exclusions [[org.clojure/tools.reader]]] nil}
user=> (require 'bidi.bidi)
Execution error (FileNotFoundException) at user/eval1756 (form-init7649922705840423474.clj:1).
Could not locate bidi/bidi__init.class, bidi/bidi.clj or bidi/bidi.cljc on classpath.
user=>
```
Versions:
- Windows 10
- Lein 2.8.3 (nREPL 0.5.3)
- see project.clj

I was able to get this to work by bringing back [https://github.com/cemerick/pomegranate/pull/102|this change] to pomegranate - this one was closed as unnecessary due to the [https://github.com/nrepl/nrepl/pull/35|corresponding change] in nREPL. Example:
```
PS C:\Users\Jack\SSD\Projects\classloaders-201> lein repl
nREPL server started on port 53166 on host 127.0.0.1 - nrepl://127.0.0.1:53166
REPL-y 0.4.3, nREPL 0.5.3
Clojure 1.10.0
Java HotSpot(TM) 64-Bit Server VM 10.0.1+10
    Docs: (doc function-name-here)
          (find-doc "part-of-name-here")
  Source: (source function-name-here)
 Javadoc: (javadoc java-object-or-class-here)
    Exit: Control+D or (exit) or (quit)
 Results: Stored in vars *1, *2, *3, an exception in *e

user=> (require 'bidi.bidi)

Execution error (FileNotFoundException) at user/eval1492 (form-init15507812266155733842.clj:1).
Could not locate bidi/bidi__init.class, bidi/bidi.clj or bidi/bidi.cljc on classpath.
user=> (require '[cemerick.pomegranate :as pg])
nil
user=> (require '[hotload.base :as lb])
nil
user=> (pg/add-dependencies :classloader (lb/find-top-classloader (lb/base-classloader-hierarchy))
  #_=>                      :coordinates '[[bidi "2.1.5"]]
  #_=>                      :repositories (merge cemerick.pomegranate.aether/maven-central {"clojars" "https://clojars.org/repo"}))
{[bidi "2.1.5"] #{[prismatic/schema "1.1.7" :exclusions [[org.clojure/tools.reader]]]}, [prismatic/schema "1.1.7" :exclusions [[org.clojure/tools.reader]]] nil}
user=> (require 'bidi.bidi)
nil
user=> bidi.bidi/ma
bidi.bidi/map->Alternates            bidi.bidi/map->IdentifiableHandler   bidi.bidi/map->Route
bidi.bidi/map->RoutesContext         bidi.bidi/map->TaggedMatch           bidi.bidi/match-beginning
bidi.bidi/match-pair                 bidi.bidi/match-pattern              bidi.bidi/match-route
bidi.bidi/match-route*               bidi.bidi/matches                    bidi.bidi/matches?
```

Pomegranate and the modification disagree on which classloader to add the new dependencies to:
```
;; this hierarchy gets longer the more evaluations are done in the repl
user=> (pg/classloader-hierarchy)
(#object[clojure.lang.DynamicClassLoader 0x3c01d248 "clojure.lang.DynamicClassLoader@3c01d248"] #object[clojure.lang.DynamicClassLoader 0x2e9d8aa7 "clojure.lang.DynamicClassLoader@2e9d8aa7"] #object[clojure.lang.DynamicClassLoader 0x2d021905 "clojure.lang.DynamicClassLoader@2d021905"] #object[clojure.lang.DynamicClassLoader 0x58c7ca08 "clojure.lang.DynamicClassLoader@58c7ca08"] #object[clojure.lang.DynamicClassLoader 0x6ae6c0ca "clojure.lang.DynamicClassLoader@6ae6c0ca"] #object[clojure.lang.DynamicClassLoader 0x286ca783 "clojure.lang.DynamicClassLoader@286ca783"] #object[clojure.lang.DynamicClassLoader 0x151b49ba "clojure.lang.DynamicClassLoader@151b49ba"] #object[clojure.lang.DynamicClassLoader 0x73ea569b "clojure.lang.DynamicClassLoader@73ea569b"] #object[clojure.lang.DynamicClassLoader 0x2fd628bc "clojure.lang.DynamicClassLoader@2fd628bc"] #object[clojure.lang.DynamicClassLoader 0x2dd21110 "clojure.lang.DynamicClassLoader@2dd21110"] #object[clojure.lang.DynamicClassLoader 0xcadd4bf "clojure.lang.DynamicClassLoader@cadd4bf"] #object[clojure.lang.DynamicClassLoader 0x12209e5e "clojure.lang.DynamicClassLoader@12209e5e"] #object[jdk.internal.loader.ClassLoaders$AppClassLoader 0x5ef04b5 "jdk.internal.loader.ClassLoaders$AppClassLoader@5ef04b5"] #object[jdk.internal.loader.ClassLoaders$PlatformClassLoader 0x2ec45662 "jdk.internal.loader.ClassLoaders$PlatformClassLoader@2ec45662"])

;; this hierarchy is stable when repeated
user=> (lb/base-classloader-hierarchy)
(#object[clojure.lang.DynamicClassLoader 0x649b9e15 "clojure.lang.DynamicClassLoader@649b9e15"] #object[clojure.lang.DynamicClassLoader 0x58186304 "clojure.lang.DynamicClassLoader@58186304"] #object[clojure.lang.DynamicClassLoader 0x57fc37e6 "clojure.lang.DynamicClassLoader@57fc37e6"] #object[clojure.lang.DynamicClassLoader 0xdd8e238 "clojure.lang.DynamicClassLoader@dd8e238"] #object[jdk.internal.loader.ClassLoaders$AppClassLoader 0x5ef04b5 "jdk.internal.loader.ClassLoaders$AppClassLoader@5ef04b5"] #object[jdk.internal.loader.ClassLoaders$PlatformClassLoader 0x2ec45662 "jdk.internal.loader.ClassLoaders$PlatformClassLoader@2ec45662"])

;; this result is stable when repeated
user=> (lb/find-top-classloader (pg/classloader-hierarchy))
#object[clojure.lang.DynamicClassLoader 0x12209e5e "clojure.lang.DynamicClassLoader@12209e5e"]

;; this result is stable when repeated but is different to the above
user=> (lb/find-top-classloader (lb/base-classloader-hierarchy))
#object[clojure.lang.DynamicClassLoader 0xdd8e238 "clojure.lang.DynamicClassLoader@dd8e238"]
```