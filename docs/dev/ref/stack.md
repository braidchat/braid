# Stack

## Braid Web Client (front-end)

. | .
--------------------------------------------- | --------
[Clojurescript](http://clojurescript.org/)    | Language
[React](https://facebook.github.io/react/)    |
[Reagent](http://reagent-project.github.io/)  |
[Re-Frame](https://github.com/Day8/re-frame)  |
[Garden](https://github.com/noprompt/garden)  | CSS
[Secretary](https://github.com/gf3/secretary) | Routing
[Figwheel](https://github.com/bhauman/lein-figwheel) | Hot Code Reload



## Braid Native Clients

. | .
-------------------------------------- | --------
[Cordova](https://cordova.apache.org/) | Mobile Client Wrapper
[Electron](http://electron.atom.io/)   | Desktop Client Wrapper


## Braid Server (back-end)

. | .
----------------------------------------------------- | --------
[Clojure](http://clojure.org/)                        | Language             
[Datomic](http://www.datomic.com/)                    | Database             
[Solr](https://lucene.apache.org/solr/)               | Search               
[Onyx](http://www.onyxplatform.org/)                  | Stream Processing    
[HTTPKit](http://www.http-kit.org/)                   | HTTP Server          
[Compojure](https://github.com/weavejester/compojure) | HTTP Routing         
[Mount](https://github.com/tolitius/mount)            | App State Management 


## In Between:

. | .
------------------------------------------------------ | --------
[Websockets](https://en.wikipedia.org/wiki/WebSocket)  | Client+Server Communication     
[Sente](https://github.com/ptaoussanis/sente)          | Client+Server Websocket Library 
[Transit](https://github.com/cognitect/transit-format) | Data Format                     



## FAQ


### Why Clojure and Clojurescript?

  decomplect
    one language for the server, web client, ios, android, and data
    LISP syntax
    functional programming
    reagent

  checks all other boxes:
    performance
      jvm
      js: unused code removal

    tooling
      figwheel
      REPL

    libraries
      interop w/ java, js libraries
      high standards
      well tested

    community
      welcoming, helpful
      innovation (ex. reagent, re-frame, figwheel)

  concerns:
    would using JS be better for the project, b/c we'd have access to more devs?


### Why Datomic?

   datalog queries are awesome
   clojure all the way down

   concerns:
     resource usage (need 4gb + RAM)
     not open source


we are considering switch to postgres:

https://github.com/braidchat/meta/issues/420


### Something something [Matrix](http://matrix.org/)!

Yes, it's on the roadmap.


### Why not [Component](https://github.com/stuartsierra/component)?

https://github.com/tolitius/mount/blob/master/doc/differences-from-component.md#differences-from-component


### Why not [Om](https://github.com/omcljs/om)?

https://github.com/braidchat/meta/issues/377

    initial version was built with om.now
    evaluated staying w/ om.now, om.next, reagent, or reagent+re-frame
      om.now:
       - ok, but lots of minor annoyances
      om.next:
        - interesting,
        - had to wrestle with it to get anything done
        - felt too much like a framework
      reagent
        - good abstraction
      reagent + re-frame
        - all the good stuff of reagent
        - plus, a global atom (as in om.now, but sans its annoyances)
        - plus, extras (interceptors)


### Why not React Native? Or Native apps?

to have something useable asap (can reuse majority of client code)

may reconsider in future
