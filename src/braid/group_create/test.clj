;; doesn't work
{::foo
      (fn []
       {})}

;; does work
{::foo
     (fn [])}

{:foo
     (fn []
   {})}

{:foo
     (identity
  {})}
