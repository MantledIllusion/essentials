# Spring 5 Lock Essentials

The lock essentials provide extensions to Spring Integration's _**LockRegistry**_ interface.

```xml
<dependency>
    <groupId>com.mantledillusion.essentials</groupId>
    <artifactId>spring5-lock-essentials</artifactId>
</dependency>
```

Get the newest version at [mvnrepository.com/spring5-lock-essentials](https://mvnrepository.com/artifact/com.mantledillusion.essentials/spring5-lock-essentials)

## Method Locks

The _@MethodLock_ and its _@ParameterLock_ annotations provided by the library allow locking a bean method against concurrent use.

They are activated by enabling Spring AOP and providing an instance of **_MethodLockAspect_** to the Spring context:

```java
@Configuration
@EnableAspectJAutoProxy
public class MyConfig {

    @Bean
    public MethodLockAspect methodLockAspect(LockRegistry lockRegistry) {
        return new MethodLockAspect(lockRegistry);
    }
}
```

### Locking a bean method

In the following example of a bean with a locked method, calls to _myMethod()_ with equal _myParamater_ values will cause a **_MethodLockedException_** to be thrown:

```java
@Component
public class MyBean {
    
    @MethodLock
    public void myMethod(@ParameterLock String myParameter) {
        ...
    }
}
```

### Locking different methods using the same lock

By default, both annotations use the method/parameter name for identification, determined through reflection. Note that in order to have parameter names at runtime, the "-parameter" option has to be set when compiling the annotated code. Otherwise, the parameters will simply be identified by arg0, arg1, etc.

Identification might be overridden though, by providing identification to the annotations:

```java
@Component
public class FirstBean {

    @MethodLock("myMethod")
    public void foo(@ParameterLock("myParameter") String bar) {
        ...
    }
}

@Component
public class SecondBean {

    @MethodLock("myMethod")
    public void bar(String someArg, @ParameterLock("myParameter") String foo) {
        ...
    }
}
```

In the example above, both methods will use the same lock if the _myParameter_ is the same, even though they have a vastly different signature.

### Distributed Locking and Synchronizing

The **_MethodLockAspect_**'s constructor requires an instance of any **_LockRegistry_** implementation. By using an implementation that shares a mutual source between services, like [Spring's own JdbcLockProvider](https://docs.spring.io/spring-integration/reference/jdbc/lock-registry.html), multiple instances of a service (or even multiple services, for that matter) in a distributed architecture are be able to share the same locks.

If needed this might be combined with retry mechanisms to archive a sort of system-wide synchronization of execution. For example, [Spring Retry](https://github.com/spring-projects/spring-retry) might be used to act on a _@MethodLock_ annotated method to perform retries when **_MethodLockedException_** is thrown:

```java
@Component
public class MyBean {

    @MethodLock
    @Retryable(MethodLockedException.class)
    public void myMethod() {
        ...
    }
}
```

Combined with a **_TimeoutRetryPolicy_** and a sensible **_BackOffPolicy_**, a call might try to retrieve a lock every 100ms for 10sec before giving up.