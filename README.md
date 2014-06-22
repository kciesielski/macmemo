MacMemo
=======

MacMemo is a simple library introducing `@memoize` macro annotation for simple function memoization. 
Annotated functions are wrapped with boilerplate code which uses **Guava CacheBuilder** to save 
returned values for given argument list.  

Memoization is scoped for particular class instance.  

Example usage:  
````scala
import scala.concurrent.duration._
import com.softwaremill.macmemo.memoize

class Worker {

    @memoize(maxSize = 20000, expiresAfter = 2 hours)
    def expensiveFunction(param: Int, param2: Seq[String]): ResultType = { ... }
}
````

Parameters (for more details see [Guava docs](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilder.html)):
* maxSize: Specifies the maximum number of entries the cache may contain.
* expiresAfter: Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the entry's creation, or the most recent replacement of its value.
* concurrencyLevel: Guides the allowed concurrency among update operations.
