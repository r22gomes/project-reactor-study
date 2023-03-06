package reactive.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

@Slf4j
public class FluxTest {

    @BeforeAll
    public static void setup(){
        BlockHound.install();

    }

    @Test
    public void fluxSubscriber() {
        Flux<String> flux = Flux.just("John", "Doe")
                .log();

        StepVerifier.create(flux)
                .expectNext("John", "Doe")
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberInteger() {
        Flux<Integer> flux = Flux.range(2, 5)
                .map(n -> n - 1)
                .log();

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberFromList() {
        Flux<Integer> flux = Flux.fromIterable(List.of(1, 2, 3, 4, 5))
                .log();

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();
    }


    @Test
    public void fluxSubscriberNumbersError() {
        Flux<Integer> flux = Flux.range(1, 5)
                .map(n -> {
                    if (n == 4) {
                        throw new IndexOutOfBoundsException("index err");
                    }
                    return n;
                })
                .log();

        flux.subscribe(i -> log.info("number {}", i), Throwable::printStackTrace,
                () -> log.info("done"));

        StepVerifier.create(flux)
                .expectNext(1, 2, 3)
                .expectError()
                .verify();
    }

    @Test
    public void fluxSubscriberNumbersErrorSubscribe3() {
        Flux<Integer> flux = Flux.range(1, 5)
                .map(n -> {
                    if (n == 4) {
                        throw new IndexOutOfBoundsException("index err");
                    }
                    return n;
                })
                .log();

        flux.subscribe(i -> log.info("number {}", i), Throwable::printStackTrace,
                () -> log.info("done"), subscription -> subscription.request(3));

    }

    @Test
    public void fluxSubscriberNumbersUgnlyBackpressure()
    {
        Flux<Integer> flux = Flux.range(1, 10)
                .log();

        flux.subscribe(new Subscriber<Integer>() {

            private int count = 0;
            private Subscription subscription;
            private int requestCount = 2;
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(2);

            }

            @Override
            public void onNext(Integer integer) {
                count ++;
                if(count >= 2){
                    count = 0;
                    subscription.request(requestCount);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        log.info("--------------------");
        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }


    @Test
    public void fluxSubscriberNumbersNotSoUglyBackpressure()
    {
        Flux<Integer> flux = Flux.range(1, 10)
                .log();

        flux.subscribe(new BaseSubscriber<Integer>() {
            private int count = 0;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(2);
            }

            @Override
            protected void hookOnNext(Integer value) {
                count ++;
                if(count >= 2){
                    count = 0;
                    request(2);
                }
            }
        });

        log.info("--------------------");
        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberIntervalVirtualTime() throws InterruptedException {
        StepVerifier.withVirtualTime(FluxTest::getIntervalFlux)
                .expectSubscription()
                .expectNoEvent(Duration.ofHours(24)) // nothing will happen in the next 24 houes
                .thenAwait(Duration.ofDays(1))
                .expectNext(0L)
                .thenAwait(Duration.ofDays(1L))
                .expectNext(1L)
                .thenCancel()
                .verify();

    }

    @Test
    public void fluxSubscriberIntegerPrettyBackpressure() {
        Flux<Integer> flux = Flux.range(2, 10)
                .map(n -> n - 1)
                .log()
                .limitRate(3);

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    public void connectableFlux() throws InterruptedException {
        ConnectableFlux<Integer> conFlux = Flux.range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish();

        // When connect is called it executes, and the subscriber subscribed the event emited at the subscription time
//        conFlux.connect(); // will start emmiting events (because of .publish)
//
//        log.info("Thread Sleeping for 300m");
//        Thread.sleep(300);
//        conFlux.subscribe(i -> log.info("s1 Number {}", i));
//
//
//        log.info("Thread Sleeping for 200m");
//        Thread.sleep(200);
//        conFlux.subscribe(i -> log.info("s2 Number {}", i));

        StepVerifier.create(conFlux)
                .then(conFlux::connect)
                .thenConsumeWhile(i -> i <= 5)
                .expectNext(6,7,8,9,10)
                .expectComplete()
                .verify();

    }

    @Test
    public void autoConnectFlux() throws InterruptedException {
        Flux<Integer> conFlux = Flux.range(1, 5)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish()
                .autoConnect(2); // Will start executing when it has at least x subs

        StepVerifier.create(conFlux) // would take for ever without two subs
                .then(conFlux::subscribe) // sub again to trigger the execution
                .expectNext(1,2,3,4,5)
                .expectComplete()
                .verify();

    }

    private static Flux<Long> getIntervalFlux() {
        return Flux.interval(Duration.ofDays(1))
                .log();
    }


}