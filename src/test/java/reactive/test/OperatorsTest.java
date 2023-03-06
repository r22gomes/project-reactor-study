package reactive.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Slf4j
public class OperatorsTest {

    @BeforeAll
    public static void setup(){
        BlockHound.install();

    }

    @Test
    public void subscribeOn() {
        Flux<Integer> flux = Flux.range(1, 4)
                .map(i -> {
                    log.info("Map 1 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(3, 4, 5, 6)
                .verifyComplete();
    }

    @Test
    public void publishOn() {
        Flux<Integer> flux = Flux.range(1, 4)
                .map(i -> {
                    log.info("Map 1 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(3, 4, 5, 6)
                .verifyComplete();
    }

    @Test
    public void multipleSubscribeOn() {
        Flux<Integer> flux = Flux.range(1, 4)
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(3, 4, 5, 6)
                .verifyComplete();
    }

    @Test
    public void multiplePublishOn() {
        Flux<Integer> flux = Flux.range(1, 4)
                .publishOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(3, 4, 5, 6)
                .verifyComplete();
    }

    // PUBLISH as precedence over subscribe
    @Test
    public void publishAndSubscribeOn() {
        Flux<Integer> flux = Flux.range(1, 4)
                .publishOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(3, 4, 5, 6)
                .verifyComplete();
    }

    @Test
    public void subscribeAndPublicOn() {
        Flux<Integer> flux = Flux.range(1, 4)
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} on thread {}", i, Thread.currentThread().getName());
                    return i + 1;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(3, 4, 5, 6)
                .verifyComplete();
    }

    @Test
    public void subscribeOnIO() throws InterruptedException {
        Mono<List<String>> list = Mono.fromCallable(() -> Files.readAllLines(Path.of("text-file")))
                .log()
                .subscribeOn(Schedulers.boundedElastic());

        list.subscribe(lines -> log.info("Line {}" + lines));

        StepVerifier.create(list)
                .expectSubscription()
                .thenConsumeWhile(l -> {
                    Assertions.assertFalse(l.isEmpty());
                    log.info("Size {}", l.size());
                    return true;
                })
                .verifyComplete();


    }


    @Test
    public void swithIfEmpty() {
        Flux<Object> flux = Flux.empty()
                .switchIfEmpty(Flux.just("NOT EMPTY"))
                .log();

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext("NOT EMPTY")
                .expectComplete()
                .verify();
    }

    @Test
    public void deferOperator() {
        Mono<Long> mono = Mono.defer(() -> Mono.just(System.currentTimeMillis()));
        // with just the time would be the same on each sub
        // DEFER EXECUTES THE VALUE INSIDE of it

        mono.subscribe(l -> log.info("{}", l));
        mono.subscribe(l -> log.info("{}", l));
        mono.subscribe(l -> log.info("{}", l));

    }

    @Test
    public void concat() {
        Flux<String> fl1 = Flux.just("a", "b")
                .delayElements(Duration.ofMillis(100));
        Flux<String> fl2 = Flux.just("c", "d");

        Flux<String> c = Flux.concat(fl1, fl2)
                .log();

        StepVerifier.create(c)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .verifyComplete();

    }

    @Test
    // executes in order
    public void concatWith() {
        Flux<String> fl1 = Flux.just("a", "b");
        Flux<String> c = Flux.just("c", "d")
                .concatWith(fl1)
                .log();

        StepVerifier.create(c)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .verifyComplete();

    }

    @Test
    public void combineLatestWith() {
        Flux<String> fl1 = Flux.just("a", "b");
        Flux<String> fl2 = Flux.just("c", "d");

        Flux<String> cl = Flux.combineLatest(fl1, fl2, (s1, s2) -> s1.toUpperCase() + s2.toUpperCase())
                .log();

        StepVerifier.create(cl)
                .expectSubscription()
                .expectNext("BC", "BD")
                .verifyComplete();
    }

    @Test
    // same as concat but subscribes eagerly does not wait for the first one to finish
    public void merge() {
        Flux<String> fl1 = Flux.just("a", "b")
                .delayElements(Duration.ofMillis(100));
        Flux<String> fl2 = Flux.just("c", "d");

        Flux<String> cl = Flux.merge(fl1, fl2)
                .log();

        StepVerifier.create(cl)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .verifyComplete();
    }

    @Test
    // same as concat but subscribes eagerly does not wait for the first one to finish
    public void mergeWith() {
        Flux<String> fl1 = Flux.just("a", "b");
        Flux<String> fl2 = Flux.just("c", "d")
                .delayElements(Duration.ofMillis(100))
                .mergeWith(fl1);


        StepVerifier.create(fl2)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }

    @Test
    // maintais the sequence
    public void mergeSequential() {
        Flux<String> fl1 = Flux.just("a", "b")
                .delayElements(Duration.ofMillis(122));
        Flux<String> fl2 = Flux.just("c", "d");

        Flux<String> merge = Flux
                .mergeSequential(fl1, fl2, fl1)
                .log();

        StepVerifier.create(merge)
                .expectSubscription()
                .expectNext("a", "b", "c", "d", "a", "b")
                .verifyComplete();
    }


    @Test
    public void concatOperatorError() {
        Flux<String> fl1 = Flux.just("a", "b")
                .map(l -> {
                    if (l.equals("b")) {
                        throw new IllegalArgumentException();
                    }
                    return l;
                });
        Flux<String> fl2 = Flux.just("c", "d");

        Flux<String> c = Flux.concatDelayError(fl1, fl2)
                .log();

        StepVerifier.create(c)
                .expectSubscription()
                .expectNext("a", "c", "d")
                .expectError()
                .verify();

    }

    @Test
    public void mergeOperatorError() {
        Flux<String> fl1 = Flux.just("a", "b")
                .map(s -> {
                    if (s.equals("b")) {
                        throw new IllegalArgumentException();
                    }
                    return s;
                })
                .doOnError(er -> log.error("COULD NOT EX"));
        Flux<String> fl2 = Flux.just("c", "d");

        Flux<String> m = Flux.mergeDelayError(1, fl1, fl2, fl1)
                .log();

        StepVerifier.create(m)
                .expectSubscription()
                .expectNext("a", "c", "d", "a")
                .expectError()
                .verify();

    }


    //
    // FLAT MAP
    //
    @Test
    public void flatMap() {
        Flux<String> fl = Flux.just("a", "b")
                .log();

        Flux<String> map = fl.map(String::toUpperCase)
                .flatMap(this::findByName)
                .log();

        StepVerifier.create(map)
                .expectSubscription()
                .expectNext("nameB1", "nameB2", "nameA1", "nameA2")
                .verifyComplete();
    }

    @Test
    public void flatMapSq() {
        Flux<String> fl = Flux.just("a", "b")
                .log();

        Flux<String> map = fl.map(String::toUpperCase)
                .flatMapSequential(this::findByName)
                .log();

        StepVerifier.create(map)
                .expectSubscription()
                .expectNext("nameA1", "nameA2", "nameB1", "nameB2")
                .verifyComplete();
    }

    public Flux<String> findByName(String name) {

        return name.equalsIgnoreCase("A")
                ? Flux.just("nameA1", "nameA2")
                .delayElements(Duration.ofMillis(100))
                : Flux.just("nameB1", "nameB2");
    }
    
    @Test
    public void zipOperator(){
        Flux<String> tt = Flux.just("film", "zito");
        Flux<String> st = Flux.just("st", "sq1");
        Flux<Integer> ep = Flux.just(12, 122);

        Flux<Show> log1 = Flux.zip(tt, st, ep)
                .flatMap(t -> Flux.just(new Show(t.getT1(), t.getT2(), t.getT3())))
                .log();

        StepVerifier.create(log1)
                .expectSubscription()
                .expectNext(new Show("film", "st", 12), new Show("zito", "sq1", 122))
                .verifyComplete();

    }

    @Test
    public void zipWithOperator(){
        Flux<Show> sh = Flux.just("film", "zito")
                .zipWith(Flux.just("st", "sq1"))
                .map(t -> new Show(t.getT1(), t.getT2(), 0))
                .log();

        StepVerifier.create(sh)
                .expectSubscription()
                .expectNext(new Show("film", "st", 0), new Show("zito", "sq1", 0))
                .verifyComplete();

    }
    
    @Data
    @AllArgsConstructor
    class Show {
        private String title;
        private String studio;
        private int eps;
    }




}
