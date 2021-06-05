package com.mygdx.newton.test;

import com.mygdx.linear.ArrayMatrix;
import com.mygdx.linear.Matrix;
import com.mygdx.methods.GoldenSectionMethod;
import com.mygdx.newton.*;
import com.mygdx.nmethods.GradientMethod;
import com.mygdx.nmethods.NMethod;
import com.mygdx.nmethods.Vector;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class NewtonTest {

    private final Random random = new Random();
    private final int size = 10;
    private final double eps = 1e-6;
    private final List<String> functions = Arrays.asList(
            "72*x^2 - 120*x*y + 72*y^2 + 12*x -30*y + 25",
            "5*x^2 + 10*y*y + 24*x + 2",
            "x*x + 2*x*y + 2*y*y + 2*x + 4*y + 3",
            "228*x*x - 144*x*y + 101*y*y -30*x + 1*y + 3"
    );
    private final List<ResearchTriple> labFunctionsFirst = Arrays.asList(
            new ResearchTriple(
                    "100*(y - x * x)*(y - x * x) + (1 - x) * (1 - x)",
                    new Vector(Arrays.asList(-1.2, 1.)),
                    new Vector(Arrays.asList(1., 1.))),
            new ResearchTriple(
                    "x * x + y * y - 6 / 5 * x * y",
                    new Vector(Arrays.asList(4., 1.)),
                    new Vector(Arrays.asList(0., 0.)))
    );
    private final List<ResearchTriple> labFunctionsSecond = Arrays.asList(
            new ResearchTriple(
                    "100*(y - x)*(y - x) + (1 - x)*(1 - x)",
                    baseStartPoint(),
                    new Vector(Arrays.asList(1., 1.))),
            new ResearchTriple(
                    "(x * x + y - 11) * (x * x + y - 11) + (x + y * y - 7) * (x + y * y - 7)",
                    baseStartPoint(),
                    new Vector(Arrays.asList(3., 2.))
            ),
            new ResearchTriple(
                    "(x + 10 * y) * (x + 10 * y) + 5 * (z - t) * (z - t) + " +
                            "(y - 2 * z) * (y - 2 * z) *(y - 2 * z) *(y - 2 * z) + " +
                            "10 * (x - t) * (x - t) * (x - t) * (x - t)",
                    new Vector(Arrays.asList(0., 0., 0., 0.)),
                    new Vector(Arrays.asList(0., 0., 0., 0.))
            )
    );

    private static class ResearchTriple {
        String func;
        Vector start;
        Vector ans;

        ResearchTriple(String func, Vector start, Vector ans) {
            this.func = func;
            this.start = start;
            this.ans = ans;
        }
    }

    @Test
    void classic() {
        functions.forEach(fun -> parseAndCheck(fun, ClassicNewtonMethod::new));
    }

    @Test
    void optimized() {
        functions.forEach(fun -> parseAndCheck(fun, f -> new OptimizedNewton<>(f, GoldenSectionMethod::new)));
    }

    @Test
    void bfsh() {
        functions.forEach(fun -> parseAndCheck(fun, BFShMethod::new));
    }

    @Test
    void powell() {
        functions.forEach(fun -> parseAndCheck(fun, PowellMethod::new));
    }

    @Test
    void cholesky() {
        final Matrix a = new ArrayMatrix(Arrays.asList(
                new Vector(Arrays.asList(4., 12., -16.)),
                new Vector(Arrays.asList(12., 37., -43.)),
                new Vector(Arrays.asList(-16., -43., 98.))));
        final CholeskyDecomposition dec = new CholeskyDecomposition(a);
        final Matrix b = dec.getL().multiply(dec.getTransposedL());
        for (int i = 0; i < a.nRows(); i++) {
            for (int j = 0; j < a.nColumns(); j++) {
                assertEquals(a.get(i, j).get(), b.get(i, j).get(), eps);
            }
        }

    }

    @Test
    void marquardtFirst() {
        functions.forEach(fun -> parseAndCheck(fun, MarquardtMethodFirst::new));
    }

    @Test
    void marquardtSecond() {
        functions.forEach(fun -> parseAndCheck(fun, MarquardtMethodSecond::new));
    }

    @Test
    void labFunctionsFirst() {
        labFunctionsFirst.forEach(this::checkLabFuncFirst);
    }

    @Test
    void labFunctionsSecond() {
        labFunctionsSecond.forEach(this::checkLabFuncSecond);
    }

    private void checkLabFuncFirst(ResearchTriple triple) {
        Function<NMethod, Double> f = method -> method.findMin(eps).add(triple.ans.multiply(-1)).length();
        assertEquals(
                0.,
                f.apply(new ClassicNewtonMethod<>(new NewtonFunction(triple.func), triple.start)),
                eps);
        assertEquals(
                0.,
                f.apply(new OptimizedNewton<>(new NewtonFunction(triple.func), triple.start)),
                eps);
    }

    private void checkLabFuncSecond(ResearchTriple triple) {
        Function<NMethod, Double> f = method -> method.findMin(eps).add(triple.ans.multiply(-1)).length();
        System.out.println("TESTING " + triple.func);
        assertEquals(
                0.,
                f.apply(new BFShMethod<>(new NewtonFunction(triple.func), triple.start)),
                eps);
        assertEquals(
                0.,
                f.apply(new PowellMethod<>(new NewtonFunction(triple.func), triple.start)),
                eps);
    }

    private ArrayMatrix generateRandomMatrix(final int n) {
        final ArrayMatrix result = new ArrayMatrix(n, n);
        IntStream.range(0, n).forEach(i -> {
            IntStream.range(0, n).forEach(j -> result.get(i, j).set(random.nextInt(n) + 1));
        });
        return result;
    }

    private void parseAndCheck(final String s, final Function<NewtonFunction, NMethod> newtonFactory) {
        System.out.println("TESTING " + s);
        final NewtonFunction f = new NewtonFunction(s);
        final NMethod method = newtonFactory.apply(f);
        final Vector expected = new GradientMethod<>(f).findMin(eps);
        final AtomicReference<Vector> res = new AtomicReference<>();
        assertTimeoutPreemptively(ofSeconds(20), () -> {
            res.set(method.findMin(eps));;
        });
        System.out.println("Iterations: " + f.getIterations());
        assertEquals(0, res.get().add(expected.multiply(-1)).length(), 10 * eps);
    }

    private static Vector baseStartPoint() {
        return new Vector(Arrays.asList(0., 0.));
    }
}
