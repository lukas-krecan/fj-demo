/**
 * Copyright 2009-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.fjdemo.parallel;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class ParallelStreamsDemo {
    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
    }


    public static void main(String[] args) {
        Stream<Integer> stream = StreamSupport.stream(wrapSpliterator(range(0, 1000).spliterator()), true);

        stream.parallel().collect(toList());
    }

    private static <T> Spliterator<T> wrapSpliterator(Spliterator<T> spliterator) {
        return new LoggingSpliteratorWrapper<>(spliterator,
                (thread, taskId, subtaskId, size, message) -> System.out.println(thread + " " + taskId + " " + subtaskId + " " + + size + " " +  message),
                0);
    }


    //"ForkJoinPool.commonPool-worker-"
}
