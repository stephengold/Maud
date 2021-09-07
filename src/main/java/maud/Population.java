/*
 Copyright (c) 2017-2021, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.noise.Generator;

/**
 * A container for elements, sorted based on a measure of fitness. Duplicate
 * fitness scores are possible, but duplicate elements are suppressed. TODO use
 * the Heart library
 *
 * @param <Fitness> type of fitness score (such as Float or ScoreDoubles, must
 * implement Comparable interface
 * @param <Element> type of elements collected (such as Solution)
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Population<Fitness extends Comparable<Fitness>, Element> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Population.class.getName());
    // *************************************************************************
    // fields

    /**
     * maximum number of elements (&gt;0, set by constructor or
     * {@link #setCapacity(int)})
     */
    private int capacity;
    /**
     * current number of elements (&ge;0)
     */
    private int numElements = 0;
    /**
     * map fitness scores to elements (not null)
     */
    final protected TreeMap<Fitness, List<Element>> elementsByFitness
            = new TreeMap<>();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a population with the specified capacity.
     *
     * @param capacity maximum number of elements (&gt;1)
     */
    public Population(int capacity) {
        Validate.positive(capacity, "capacity");
        this.capacity = capacity;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a single element to this population (unless it is a duplicate).
     *
     * @param element instance to add (not null)
     * @param score (may be null)
     */
    public void add(Element element, Fitness score) {
        Validate.nonNull(element, "element");

        if (numElements >= capacity) {
            assert numElements == capacity : numElements;
            if (worstScore().compareTo(score) >= 0) {
                return;
            }
        }

        List<Element> list = elementsByFitness.get(score);
        if (list == null) {
            list = new ArrayList<>(1);
            List<Element> previousList = elementsByFitness.put(score, list);
            assert previousList == null;
        } else {
            assert !list.isEmpty();
        }

        if (!list.contains(element)) {
            list.add(element);
            numElements++;
            cull(capacity);
        }

        if (list.isEmpty()) {
            list = elementsByFitness.remove(score);
            assert list != null;
        }
    }

    /**
     * Add a list of elements (all with the same fitness score) to this
     * population, excluding duplicates.
     *
     * @param addList list of elements to add (not null, all elements non-null)
     * @param score (may be null)
     */
    public void add(List<Element> addList, Fitness score) {
        Validate.nonNull(addList, "list");

        if (numElements >= capacity) {
            assert numElements == capacity : numElements;
            if (worstScore().compareTo(score) >= 0) {
                return;
            }
        }

        int addCount = addList.size();
        List<Element> list = elementsByFitness.get(score);
        if (list == null) {
            list = new ArrayList<>(addCount);
            List<Element> previousList = elementsByFitness.put(score, list);
            assert previousList == null;
        } else {
            assert !list.isEmpty();
        }

        for (Element element : addList) {
            if (!list.contains(element)) {
                list.add(element);
                numElements++;
            }
        }
        if (list.isEmpty()) {
            list = elementsByFitness.remove(score);
            assert list != null;
        }
        cull(capacity);
    }

    /**
     * Find the highest fitness score in this population.
     *
     * @return the pre-existing instance or null
     * @see #fittest()
     * @see #worstScore()
     */
    public Fitness bestScore() {
        Map.Entry<Fitness, List<Element>> bestEntry
                = elementsByFitness.lastEntry();
        if (bestEntry == null) {
            return null;
        }
        Fitness result = bestEntry.getKey();

        return result;
    }

    /**
     * Cull this population, based solely on fitness, until no more than the
     * specified number of elements remain.
     *
     * @param targetSize target number of elements (&ge;0)
     */
    public void cull(int targetSize) {
        Validate.nonNegative(targetSize, "target size");

        while (numElements > targetSize) {
            Fitness worst = elementsByFitness.firstKey();
            List<Element> list = elementsByFitness.get(worst);
            int listSize = list.size();
            assert listSize > 0 : listSize;
            if (numElements - listSize >= targetSize) {
                List<Element> old = elementsByFitness.remove(worst);
                assert old != null;
                numElements -= listSize;
            } else {
                Iterator<Element> it = list.iterator();
                while (it.hasNext()) {
                    it.next();
                    if (numElements > targetSize) {
                        it.remove();
                        numElements--;
                    }
                }
            }
        }
    }

    /**
     * Find the fittest (highest-scoring) element in this population.
     *
     * @return the pre-existing instance or null if none
     */
    public Element fittest() {
        Map.Entry<Fitness, List<Element>> bestEntry
                = elementsByFitness.lastEntry();
        if (bestEntry == null) {
            return null;
        }
        List<Element> bestElements = bestEntry.getValue();
        assert !bestElements.isEmpty();
        Element result = bestElements.get(0);

        return result;
    }

    /**
     * Read the capacity of this population.
     *
     * @return number of elements (&gt;0)
     * @see #size()
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Enumerate all elements in the population, in descending order.
     *
     * @return a new list of elements
     */
    public List<Element> listElements() {
        List<Element> result = new ArrayList<>(numElements);
        for (Entry<Fitness, List<Element>> entry
                : elementsByFitness.descendingMap().entrySet()) {
            List<Element> list = entry.getValue();
            assert !list.isEmpty();
            result.addAll(list);
        }

        return result;
    }

    /**
     * Merge the fittest elements into another population.
     *
     * @param maxCount maximum number of elements to merge (&ge;0)
     * @param destination population to merge into (not null, modified)
     * @return the number of elements merged (&ge;0, &le;maxCount)
     */
    public int mergeFittestTo(int maxCount,
            Population<Fitness, Element> destination) {
        Validate.nonNegative(maxCount, "maxCount");
        Validate.nonNull(destination, "destination");

        int numMerged = 0;
        for (Entry<Fitness, List<Element>> entry
                : elementsByFitness.descendingMap().entrySet()) {
            List<Element> list = entry.getValue();
            assert !list.isEmpty();
            Fitness score = entry.getKey();
            if (numMerged + list.size() <= maxCount) {
                destination.add(list, score);
                numMerged += list.size();
            } else {
                Iterator<Element> it = list.iterator();
                while (numMerged < maxCount) {
                    Element element = it.next();
                    destination.add(element, score);
                    numMerged++;
                }
                return numMerged;
            }
        }
        return numMerged;
    }

    /**
     * Merge the specified subset of this population into another population.
     *
     * @param subset set bits specify which elements to merge, bit 0 indicating
     * the least fit element (not null, unaffected)
     * @param destination population to merge into (not null, modified)
     * @return count of elements merged (&ge;0, &le;maxCount)
     */
    public int mergeSubsetTo(BitSet subset,
            Population<Fitness, Element> destination) {
        Validate.nonNull(subset, "subset");
        Validate.nonNull(destination, "destination");

        int currentIndex = 0;
        int nextIndex = subset.nextSetBit(0);
        if (nextIndex == -1) {
            return 0;
        }

        int numMerged = 0;
        for (Entry<Fitness, List<Element>> entry
                : elementsByFitness.entrySet()) {
            List<Element> list = entry.getValue();
            assert !list.isEmpty();
            if (currentIndex + list.size() <= nextIndex) {
                currentIndex += list.size();
            } else {
                Fitness score = entry.getKey();
                for (Element element : list) {
                    if (currentIndex == nextIndex) {
                        destination.add(element, score);
                        numMerged++;
                        nextIndex = subset.nextSetBit(nextIndex + 1);
                        if (nextIndex == -1) {
                            return numMerged;
                        }
                    }
                    currentIndex++;
                }
            }
        }
        /*
         * more bits than elements
         */
        return numMerged;
    }

    /**
     * Merge all elements into another population.
     *
     * @param destination population to merge into (not null, modified)
     */
    public void mergeTo(Population<Fitness, Element> destination) {
        Validate.nonNull(destination, "destination");

        for (Entry<Fitness, List<Element>> entry
                : elementsByFitness.entrySet()) {
            List<Element> list = entry.getValue();
            assert !list.isEmpty();
            Fitness score = entry.getKey();
            destination.add(list, score);
        }
    }

    /**
     * Merge a uniformly-distributed sample into another population.
     *
     * @param maxCount maximum number of elements to merge (&ge;0)
     * @param generator pseudo-random generator to use (not null)
     * @param destination (not null, modified)
     * @return count of elements merged (&ge;0, &le;maxCount)
     */
    public int mergeUniformTo(int maxCount, Generator generator,
            Population<Fitness, Element> destination) {
        Validate.nonNegative(maxCount, "maxCount");
        Validate.nonNull(generator, "generator");
        Validate.nonNull(destination, "destination");

        if (maxCount >= numElements) {
            mergeTo(destination);
            return numElements;
        }
        BitSet selected = new BitSet(numElements);
        int lastIndex = numElements - 1;
        for (int i = 0; i < maxCount; i++) {
            int bitIndex = generator.pick(selected, lastIndex, false);
            assert bitIndex != -1;
            selected.set(bitIndex);
        }
        int result = mergeSubsetTo(selected, destination);

        return result;
    }

    /**
     * Alter the capacity of this container.
     *
     * @param newCapacity number of elements (&gt;0)
     * @see #cull(int)
     * @see #getCapacity()
     */
    public void setCapacity(int newCapacity) {
        Validate.positive(newCapacity, "new capacity");

        capacity = newCapacity;
        cull(capacity);
    }

    /**
     * Read the number of elements in this population.
     *
     * @return count of elements (&ge;0)
     * @see #getCapacity()
     */
    public int size() {
        assert numElements >= 0 : numElements;
        assert numElements <= capacity : numElements;
        assert numElements >= elementsByFitness.size() : numElements;
        return numElements;
    }

    /**
     * Find the lowest score in this population.
     *
     * @return the pre-existing instance or null
     * @see #bestScore()
     */
    public Fitness worstScore() {
        Map.Entry<Fitness, List<Element>> worstEntry
                = elementsByFitness.firstEntry();
        if (worstEntry == null) {
            return null;
        }
        Fitness result = worstEntry.getKey();

        return result;
    }
}
