MacMemo
=======

This is alpha release of MacMemo, a macro-based utility for function memoization.

Usage: Annotate function with @memoize to make it remember last 1000 calls for at most 5 seconds.

Notice: Currently the memoization lasts only in object-scope, so calling the same function in different objects will not be remembered in same scope.

TODO: 
* Parametrization of buffer size and TTL
* Check if it works for some nontrivial (traits, objects, recursion, closures, etc.)
* Check how to keep cache in wider scope
* Extend this README ;)
