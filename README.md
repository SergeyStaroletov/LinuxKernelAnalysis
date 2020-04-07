# Linux Kernel Analysis using Levenshtein Distance and JGit

Suppose  we  have  a  local  Git  repository  of  the  Kernel or  Kernel  related  sources  
(but  it  can  be  applied  to  any  Git repository too).

Our goal is to:

1)Find  the  most  frequent  errors.  It  could  be  reformulated as: find the most common commit messages of fixes, and 
then reformulated to: find the most relevantcommit messages of fixes, not necessary the same but similar.

2)Find  the  most  buggy  sources.  It  could  be  reformulated  to:  find  the  files  which  were  mentioned  in  
thecommits with ”fix” message most often.

3)Find  the  most  buggy  lines  of  code.  It  could  bereformulated to: find the lines which were mentioned 
in  the  fixing  commit  messages  in  the  most  buggysource files most often.


Solving the first problem will able us to getting know the most regular classes of the errors in C Kernel code. 
Developers and  teachers  should  be  aware  of  them  to  learn  C  techniques 
and kernel programming to avoid them.

Solving the second problem will show us the most unstable portions of the Kernel, 
it could be very hard to write them at once  correctly  because  of  difficulty.  

Also,  it  could  mean  that the component corresponding to file is an active development and fixing because of significance.
And solving the third program can be useful for analysingthe errors with respect to a source code 


Related publications: 

(PDF) A survey of most common errors in Linux Kernel. 
Available from: https://www.researchgate.net/publication/324834629_A_survey_of_most_common_errors_in_Linux_Kernel 

In Russian with some new data:
http://samag.ru/archive/article/3859
