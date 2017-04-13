# Contributing: Coding



## Editor

If you're just starting out with Clojure(script):
  - if you use vim or emacs, stick with that
  - otherwise, we recommend Cursive: https://cursive-ide.com/ (over Lighttable, SublimeText, Atom, etc.)


Fork the Project

Clone your fork locally

Install Packages


[./how-to/getting-up-and-running-in-development.md](./how-to/getting-up-and-running-in-development.md)




let us know what features you plan to work on
PR early, to start a discussion (and avoid major refactoring)


Issues

https://github.com/braidchat/planning/issues

 Check for open issues or open a fresh issue to start a discussion around a feature idea or a bug

 There is a [Help Wanted](https://github.com/braidchat/planning/issues?q=is%3Aissue+is%3Aopen+label%3Ahelp-wanted) tag for issues that should be ideal for people who are not very familiar with the codebase yet.


## Support

If at any point, you run into problems, jump into the [braid group](http://braid.chat/group/braid).

@jamesnvc and @rafd have a standing offer to remotely pair-code with anyone on Braid (via [Screenhero]() or [Teamviewer]()). Message us if you'd like to take us up on it.





Feature Branches

`git checkout -b new-shiny-thing`



Commit Best Practices

- messages: chris.beams.io/posts/git-commit/
-


Pull in Upstream Change Regularly

```
git remote add upstream ...
git fetch upstream
git merge upstream/master --ff
```


Make a Pull Request

```
git push origin new-shiny-thing
```



Add yourself to [CONTRIBUTORS.md](../CONTRIBUTORS.edn)


