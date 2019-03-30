package primes;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.IntStream;

public class SieveOfEratosthenes extends PrimeSieve<Integer>
{
	private static final long serialVersionUID = 2517820765374237247L;
	private static final int MAX_LENGTH = 1 << 30;
	private static final int FIRST_PRIME = 2;
	
	private static void checkBound(int bound)
	{
		if (bound > MAX_LENGTH)
		{
			throw new IllegalArgumentException("Cannot extend sieve beyond 2^30.");
		}
	}
	
	private BitSet sieve;
	private int[] waypoints = new int[4];
	private int wpPopulation = 0;
	private int wpInterval = 1;
	private int wpThreshold = 4;
	private int wpCounter = 0;
	private int wp = 0;
	private int upperBound = 1;
	
	public SieveOfEratosthenes(int bound)
	{
		checkBound(bound);
		sieve = new BitSet(bound + 1);
		growSieve(bound);
	}
	
	public SieveOfEratosthenes()
	{
		this(1 << 14);
	}
	
	private void growSieve(int newBound)
	{
		checkBound(newBound);
		if (newBound > upperBound)
		{
			int squareRoot = (int)Math.sqrt(newBound);
			sieve.flip(upperBound + 1, newBound + 1);
			for (int p = FIRST_PRIME; p != -1; p = sieve.nextSetBit(p + 1))
			{
				if (p <= squareRoot)
				{
					int i = p < upperBound ? upperBound + p - (upperBound % p) : 2*p;
					while (i <= newBound)
					{
						sieve.clear(i);
						i += p;
					}
				}
				if (p > upperBound)
				{
					if (wpCounter++ % wpInterval == 0)
					{
						waypoints[wp++] = p;
						wpPopulation++;
					}
					if (wpPopulation == wpThreshold)
					{
						growWaypoints();
					}
				}
			}
			upperBound = newBound;
		}
	}
	
	private void growWaypoints()
	{
		wpInterval *= 2;
		wpThreshold *= 2;
		int[] expandedWaypoints = new int[wpThreshold];
		for (int i = 0; i < waypoints.length; i += 2)
		{
			expandedWaypoints[i / 2] = waypoints[i];
		}
		waypoints = expandedWaypoints;
		wpPopulation /= 2;
		wp /= 2;
	}
	
	@Override
	public Integer firstPrime()
	{
		return FIRST_PRIME;
	}
	
	@Override
	public Integer lastPrime()
	{
		return sieve.length() - 1;
	}
	
	@Override
	public Integer[] bounds()
	{
		return new Integer[]{1, upperBound};
	}
	
	@Override
	public void extendTo(Integer bound)
	{
		growSieve(bound);
		incrModCount();
	}
	
	@Override
	public Optional<Integer> nextPrime(Integer n)
	{
		int p = sieve.nextSetBit(n + 1);
		return p != -1 ? Optional.of(p) : Optional.empty();
	}
	
	@Override
	public Optional<Integer> previousPrime(Integer n)
	{
		int p = sieve.previousSetBit(n - 1);
		return p != -1 ? Optional.of(p) : Optional.empty();
	}
	
	@Override
	public int size()
	{
		return sieve.cardinality();
	}
	
	@Override
	public boolean isEmpty()
	{
		return upperBound < 2;
	}
	
	@Override
	public boolean contains(Object obj)
	{
		if (obj instanceof Integer)
		{
			int p = (int)obj;
			return p > 0 && p <= lastPrime() && sieve.get(p);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public Integer get(int index)
	{
		Objects.checkIndex(index, size());
		int p = waypoints[index / wpInterval];
		for (int i = 0; i < index % wpInterval; i++)
		{
			p = sieve.nextSetBit(p + 1);
		}
		return p;
	}
	
	@Override
	public int indexOf(Object obj)
	{
		if (contains(obj))
		{
			int index = 0;
			int waypoint = 0;
			int p = (int)obj;
			if (p > waypoints[wpPopulation - 1])
			{
				waypoint = lastPrime();
				index = size() - 1;
			}
			else
			{
				for (int i = 0; i < wpPopulation; i++)
				{
					if (waypoints[i] >= p)
					{
						index = i * wpInterval;
						waypoint = waypoints[i];
						break;
					}
				}
			}
			for (int n = waypoint; n > p; n = sieve.previousSetBit(n - 1))
			{
				index--;
			}
			return index;
		}
		else
		{
			return -1;
		}
	}
	
	@Override
	public Iterator<Integer> iterator()
	{
		return new Iterator<>()
		{
			private int expectedModCount = getModCount();
			private Iterator<Integer> subIter = sieve.stream().boxed().iterator();
			
			@Override
			public boolean hasNext()
			{
				return subIter.hasNext();
			}
			
			@Override
			public Integer next()
			{
				checkNotModified(expectedModCount);
				return subIter.next();
			}
		};
	}
	
	@Override
	public Spliterator<Integer> spliterator()
	{
		return Spliterators.spliterator(this, spliteratorCharacteristics | Spliterator.SORTED);
	}
	
	@Override
	public ListIterator<Integer> listIterator(int index)
	{
		return new ListIterator<>()
		{
			private int nextPrime = get(index);
			private int prevPrime = sieve.previousSetBit(nextPrime - 1);
			private int i = index;
			private int expectedModCount = getModCount();
			
			@Override
			public boolean hasNext()
			{
				return nextPrime != -1;
			}
			
			@Override
			public boolean hasPrevious()
			{
				return prevPrime != -1;
			}
			
			@Override
			public Integer next()
			{
				if (!hasNext())
				{
					throw new NoSuchElementException();
				}
				checkNotModified(expectedModCount);
				prevPrime = nextPrime;
				nextPrime = sieve.nextSetBit(nextPrime + 1);
				i++;
				return prevPrime;
			}
			
			@Override
			public int nextIndex()
			{
				return i;
			}
			
			@Override
			public Integer previous()
			{
				if (!hasPrevious())
				{
					throw new NoSuchElementException();
				}
				checkNotModified(expectedModCount);
				nextPrime = prevPrime;
				prevPrime = sieve.previousSetBit(prevPrime - 1);
				i--;
				return nextPrime;
			}
			
			@Override
			public int previousIndex()
			{
				return i - 1;
			}
			
			@Override
			public void add(Integer e)
			{
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void set(Integer e)
			{
				throw new UnsupportedOperationException();
			}
		};
	}
	
	@Override
	public ListIterator<Integer> listIterator()
	{
		return listIterator(0);
	}
	
	@Override
	public Stream<Integer> composites()
	{
		int expectedModCount = getModCount();
		int[] bounds = {bounds()[0], bounds()[1]};
		return IntStream.iterate(
			sieve.nextClearBit(bounds[0] + 1),
			n -> n <= bounds[1],
			n -> {checkNotModified(expectedModCount); return sieve.nextClearBit(n + 1);}).boxed();
	}
	
	@Override
	public Object[] toArray()
	{
		return stream().toArray();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a)
	{
		T[] array = Arrays.copyOf(a, size());
		Iterator<Integer> iter = iterator();
		try
		{
			for (int i = 0; i < array.length; i++)
			{
				array[i] = (T)iter.next();
			}
		}
		catch (ClassCastException exc)
		{
			throw new ArrayStoreException();
		}
		return array;
	}
	
	@Override
	public void clear()
	{
		sieve = new BitSet(2);
		waypoints = new int[4];
		wpPopulation = 0;
		wpInterval = 1;
		wpThreshold = 4;
		wpCounter = 0;
		wp = 0;
		upperBound = 1;
		incrModCount();
	}
}