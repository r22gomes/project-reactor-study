package reactive.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@Slf4j
// MONO é uma stream de um elemento, ou vazia
public class MonoTest {

    @BeforeAll
    public static void setup(){
        BlockHound.install();

    }

    @Test
    public void blockHoundWorks(){
        try{
            Mono.delay(Duration.ofSeconds(1))
                    .doOnNext(it -> {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .block();
        }catch (Exception e){
            Assertions.assertTrue(e.getCause() instanceof BlockingOperationError);
        }
    }

    @Test
    public void monoSubscriber(){
        String name = "John Doe";
        Mono<String> stringMono = Mono.just(name)
                .log();

        stringMono.subscribe();
        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                        .expectNext("John Doe")
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoSubscriberConsumer(){
        String name = "John Doe";
        Mono<String> stringMono = Mono.just(name)
                .log();

        stringMono.subscribe(s -> log.info("value {}", s));
        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .expectNext("John Doe")
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoSubscribeError(){
        String name = "John Doe";
        Mono<String> stringMono = Mono.just(name)
                .map(s -> {
                    throw new RuntimeException("testing error mono");
                });

        stringMono.subscribe(s -> log.info("value {}", s), t -> log.error("there was an error - {}", t.getLocalizedMessage()));
        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .verifyError(RuntimeException.class);
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoSubscriberConsumerComplete(){
        String name = "John Doe";
        Mono<String> stringMono = Mono.just(name)
                .log()
                .map(String::toUpperCase);

        stringMono.subscribe(s -> log.info("value {}", s));
        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .expectNext("JOHN DOE")
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoSubscriberConsumerSubscription(){
        String name = "John Doe";
        Mono<String> stringMono = Mono.just(name)
                .log()
                .map(String::toUpperCase);

        stringMono.subscribe(s -> log.info("value {}", s), Throwable::printStackTrace, () -> log.info("finished"), Subscription::cancel);
        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .expectNext("JOHN DOE")
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }


    @Test
    public void monoDoOn(){
        String name = "John Doe";
        Mono<Object> stringMono = Mono.just(name)
                .log()
                .map(String::toUpperCase)
                .doOnSubscribe(s -> log.info("subscribed"))
                .doOnRequest(r -> log.info("requested received {}", r) )
                .flatMap(s -> Mono.empty())
                .doOnNext(s -> log.info("on NExt {}", s))
                .doOnSuccess(s -> log.info("success {}", s));

        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoDoOnErr(){
        String name = "John Doe";
        Mono<Object> stringMono = Mono.error(new IllegalArgumentException("error ill arg"))
                .doOnError(s -> log.error("ERROR FOUND {}", s.getMessage()))
                .doOnNext(s -> log.info("do on next"))
                .log();

        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .verifyError();
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoDoOnErrResume(){
        String name = "John Doe";
        Mono<Object> stringMono = Mono.error(new IllegalArgumentException("error ill arg"))
                .onErrorResume(s -> Mono.just("data"))
                .doOnNext(s -> log.info("do on next triggered"))
                .log();

        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .expectNext("data")
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }

    @Test
    public void monoDoOnErrReturn(){
        String name = "John Doe";
        Mono<Object> stringMono = Mono.error(new IllegalArgumentException("error ill arg"))
                .onErrorReturn("retErr")
                .log();

        log.info("---ASSERTIONS---");
        StepVerifier.create(stringMono)
                .expectNext("retErr")
                .verifyComplete();
        log.info("Mono {}", stringMono);
    }


}
