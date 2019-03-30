package primes;

import java.util.*;
import java.util.stream.Stream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigInteger;
import static java.util.Spliterator.*;

/*
 * A list of the primes in some interval. Can only be extended or cleared; all other mutator methods
 * are disabled.
 *
 * The elements of a sieve are only primes, not all the numbers processed. Unless explicitly
 * stated otherwise, information provided about the elements of a sieve is given in this context.
 * An argument to a constructor, however, can be any positive integer that fits into a variable of
 * type `N`.
 *
 * Iterators and their variants (e.g. spliterators) should be fail-fast unless the subclass is
 * immutable or safe for concurrent operations. Making a subclass immutable simply requires disabling
 * `extendTo` and `clear`, so if these operations are not required, it is highly recommended that they
 * be disabled. However, to maintain consistency among subclasses, either both methods should be
 * disabled or neither should be. In other words, subclasses should be either fully mutable to the
 * extent allowed by this class, or immutable.
 */
public abstract class PrimeSieve<N extends Number> implements List<N>, Serializable
{
	protected static final int spliteratorCharacteristics = DISTINCT | NONNULL | ORDERED;
	
	private transient int modCount = 0;
	
	/*
	 * A view of a portion of the primes in the sieve.
	 *
	 * The default implementation cannot be directly mutated, though it is not technically immutable
	 * unless its enclosing sieve is immutable.
	 */
	protected class View extends PrimeSieve<N>
	{
		private final N first;
		private final N last;
		private final int fromIndex;
		private final int toIndex;
		
		// An exception will be thrown if an empty view is requested.
		protected View(int fromIndex, int toIndex)
		{
			Objects.checkFromToIndex(fromIndex, toIndex, PrimeSieve.this.size());
			if (toIndex - fromIndex == 0)
			{
				throw new IllegalArgumentException("Sublist is empty.");
			}
			first = PrimeSieve.this.get(fromIndex);
			last = toIndex - fromIndex > 1 ? PrimeSieve.this.get(toIndex - 1) : first;
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
		
		private boolean inBounds(int index)
		{
			return index >= fromIndex && index < toIndex;
		}
		
		protected boolean inBounds(N n)
		{
			BigInteger num = null;
			BigInteger lowerBound = null;
			BigInteger upperBound = null;
			if (n instanceof BigInteger)
			{
				num = (BigInteger)n;
				lowerBound = (BigInteger)first;
				upperBound = (BigInteger)last;
			}
			else
			{
				num = BigInteger.valueOf(n.longValue());
				lowerBound = BigInteger.valueOf(first.longValue());
				upperBound = BigInteger.valueOf(last.longValue());
			}
			return num.compareTo(lowerBound) >= 0 && num.compareTo(upperBound) <= 0;
		}
		
		protected final int getFromIndex()
		{
			return fromIndex;
		}
		
		protected final int getToIndex()
		{
			return toIndex;
		}
		
		@Override
		public final N firstPrime()
		{
			return first;
		}
		
		@Override
		public final N lastPrime()
		{
			return last;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public final N[] bounds()
		{
			N[] result = (N[])Array.newInstance(first.getClass(), 2);
			result[0] = first;
			result[1] = last;
			return result;
		}
		
		@Override
		public Optional<N> nextPrime(N n)
		{
			return inBounds(n)
				? PrimeSieve.this.nextPrime(n).filter(this::inBounds)
				: Optional.empty();
		}
		
		@Override
		public Optional<N> previousPrime(N n)
		{
			return inBounds(n)
				? PrimeSieve.this.previousPrime(n).filter(this::inBounds)
				: Optional.empty();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object obj)
		{
			return PrimeSieve.this.contains(obj) && inBounds((N)obj);
		}
		
		@Override
		public N get(int index)
		{
			Objects.checkIndex(index, size());
			return PrimeSieve.this.get(index + fromIndex);
		}
		
		@Override
		public int indexOf(Object obj)
		{
			int index = PrimeSieve.this.indexOf(obj);
			return inBounds(index) ? index - fromIndex : -1;
		}
		
		@Override
		public final int size()
		{
			return toIndex - fromIndex;
		}
		
		@Override
		public final boolean isEmpty()
		{
			return false;
		}
		
		@Override
		public Iterator<N> iterator()
		{
			return new Iterator<>()
			{
				private N current = first;
				private int expectedModCount = getModCount();
				
				@Override
				public boolean hasNext()
				{
					return current != null;
				}
				
				@Override
				public N next()
				{
					if (!hasNext())
					{
						throw new NoSuchElementException();
					}
					checkNotModified(expectedModCount);
					N temp = current;
					current = nextPrime(current).orElse(null);
					return temp;
				}
			};
		}
		
		@Override
		public ListIterator<N> listIterator(int index)
		{
			return new ListIterator<>()
			{
				private int i = fromIndex + index;
				private int expectedModCount = getModCount();
				private N current = get(index);
				private N prev = previousPrime(current).orElse(null);
				
				@Override
				public boolean hasNext()
				{
					return i < toIndex;
				}
				
				@Override
				public boolean hasPrevious()
				{
					return i > fromIndex;
				}
				
				@Override
				public N next()
				{
					if (!hasNext())
					{
						throw new NoSuchElementException();
					}
					checkNotModified(expectedModCount);
					prev = current;
					current = PrimeSieve.this.nextPrime(current).orElse(null);
					i++;
					return prev;
				}
				
				@Override
				public N previous()
				{
					if (!hasPrevious())
					{
						throw new NoSuchElementException();
					}
					checkNotModified(expectedModCount);
					current = prev;
					prev = PrimeSieve.this.previousPrime(prev).orElse(null);
					i--;
					return current;
				}
				
				@Override
				public int nextIndex()
				{
					return i - fromIndex;
				}
				
				@Override
				public int previousIndex()
				{
					return nextIndex() - 1;
				}
				
				@Override
				public void add(N e)
				{
					throw new UnsupportedOperationException();
				}
				
				@Override
				public void remove()
				{
					throw new UnsupportedOperationException();
				}
				
				@Override
				public void set(N e)
				{
					throw new UnsupportedOperationException();
				}
			};
		}
		
		@Override
		public ListIterator<N> listIterator()
		{
			return listIterator(0);
		}
		
		@Override
		public Spliterator<N> spliterator()
		{
			return Spliterators.spliterator(this, PrimeSieve.this.spliterator().characteristics());
		}
		
		// `filter` is used instead of `dropWhile` and `takeWhile` because those methods are extremely
		// slow for `PrimeSieve.parallelStream`.
		@Override
		public Stream<N> composites()
		{
			return PrimeSieve.this.composites().filter(this::inBounds);
		}
		
		// Returns another view of the main sieve, not a view of this view.
		@Override
		public PrimeSieve<N> subList(int fromIndex, int toIndex)
		{
			Objects.checkFromToIndex(fromIndex, toIndex, size());
			return PrimeSieve.this.subList(fromIndex + this.fromIndex, toIndex + this.fromIndex);
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
			Iterator<N> iter = iterator();
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
		public final void extendTo(N bound)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public final void clear()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	// The following three methods can be used to facilitate fail-fast iteration.
	
	protected int getModCount()
	{
		return modCount;
	}
	
	protected void incrModCount()
	{
		modCount++;
	}
	
	protected void checkNotModified(int expectedModCount)
	{
		if (expectedModCount != modCount)
		{
			throw new ConcurrentModificationException();
		}
	}
	
	/*
	 * Extends the sieve to cover all primes less than or equal to the given upper bound, which
	 * may be either prime or composite.
	 */
	public abstract void extendTo(N bound);
	
	public abstract N firstPrime();
	
	public abstract N lastPrime();

	/* Returns the closed interval of positive integers that have been sieved. */
	public abstract N[] bounds();
	
	/*
	 * Returns an `Optional` containing the least prime greater than `n` if `n` is within the
	 * sieved interval, otherwise an empty `Optional`.
	 */
	public abstract Optional<N> nextPrime(N n);
	
	/*
	 * Returns an `Optional` containing the greatest prime less than `n` if `n` is within the
	 * sieved interval, otherwise an empty `Optional`.
	 */
	public abstract Optional<N> previousPrime(N n);
	
	/* Returns a fail-fast stream containing the composite numbers that have been sieved. */
	public abstract Stream<N> composites();
	
	/*
	 * If `index` is within bounds, returns an `Optional` containing that prime. If `index` is
	 * greater than or equal to the amount of primes in the sieve, returns an empty `Optional`.
	 * Otherwise, throws `IndexOutOfBoundsException`.
	 */
	public Optional<N> getIfPresent(int index)
	{
		try
		{
			return Optional.of(get(index));
		}
		catch (IndexOutOfBoundsException exc)
		{
			if (index < 0)
			{
				throw exc;
			}
			else
			{
				return Optional.empty();
			}
		}
	}
	
	@Override
	public boolean containsAll(Collection<?> c)
	{
		return c.stream().allMatch(this::contains);
	}
	
	@Override
	public final int lastIndexOf(Object obj)
	{
		return indexOf(obj);
	}
	
	@Override
	public Spliterator<N> spliterator()
	{
		return Spliterators.spliterator(this, spliteratorCharacteristics);
	}
	
	@Override
	public PrimeSieve<N> subList(int fromIndex, int toIndex)
	{
		return this.new View(fromIndex, toIndex);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof List<?>)
		{
			List<?> otherList = (List<?>)obj;
			if (size() == otherList.size())
			{
				Iterator<N> thisIter = iterator();
				Iterator<?> otherIter = otherList.iterator();
				while (thisIter.hasNext() && otherIter.hasNext())
				{
					if (!thisIter.next().equals(otherIter.next()))
					{
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		int result = 1;
		for (N p: this)
		{
			result *= 31;
			result += p.hashCode();
		}
		return result;
	}
	
	@Override
	public String toString()
	{
		switch (size())
		{
			case 0:
				return "[]";
			case 1:
			case 2:
			case 3:
				StringBuilder formattedElements = new StringBuilder("[");
				for (N p: this)
				{
					formattedElements.append(p);
					formattedElements.append(", ");
				}
				int len = formattedElements.length();
				return formattedElements.replace(len - 2, len, "]").toString();
			default:
				return String.format("[%s, %s, ..., %s, %s]", firstPrime(),
					nextPrime(firstPrime()).get(), previousPrime(lastPrime()).get(), lastPrime());
		}
	}
	
	@Override
	public final void add(int index, N element)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final boolean add(N e)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final boolean addAll(int index, Collection<? extends N> c)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final boolean addAll(Collection<? extends N> c)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final N remove(int index)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final boolean remove(Object o)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final N set(int index, N element)
	{
		throw new UnsupportedOperationException();
	}
}