## hadoop/
There are 3 Hadoop applications that selected from the hadoop 1.0.4 release package:
- Word Count : Count the number of different words from 200 MB of text file. The job contains 4 map tasks, 1 reduce task. Marked sensitive variable: The return count result
- Pi : Estimate the value of 𝜋 using the Monte-Carlo method. The job contains 10 map tasks, where each task generates 100,000 samples. Marked sensitive variable: The final result 𝜋
- Tera Sort : Sort 1 GB of text strings in the alphabetical order. The job contains 18 map tasks and 1 reduce task. Marked sensitive variable: The number of strings

## java/
There are 7 java CPU-intensive applications:
- Heap Sort : Sort an array of 100 elements using heap sort. Marked sensitive variable: The array to be sorted
- Bubble Sort : Sort an array of 100 elements using bubble sort.  Marked sensitive variable: The array to be sorted
- Quick Sort : Sort an array of 100 elements using quick sort.  Marked sensitive variable: The array to be sorted
- Binary Search : Find the position of a given number in an array of 100 elements.  Marked sensitive variable: The given array
- KMP : Find the position of a given pattern within a string of 100 characters. Marked sensitive variable: The matched string
- Fast Power : Quickly compute the 𝑛th power of a given base number. Here we calculate 2 to the 20th power. Marked sensitive variable: The last generated result
- Pi : Use the Monte Carlo algorithm to find the value of 𝜋, and the number of samples is 100000. Marked sensitive variable: The last generated result
