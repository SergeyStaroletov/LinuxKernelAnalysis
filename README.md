# Linux Kernel Analysis using Levenshtein Distance and JGit

Suppose  we  have  a  local  Git  repository  of  the  Kernel  or Kernel related sources (but the presenting technique can also be applied to any Git repository). Our goals are:

1)  Find the most frequent errors. It could be known as: find the  most  common  commit  messages  of  fixes,  and  then reformulated to: find the most relevant commit messages of fixes, not necessary the same but similar.
2)  Find  the  most  buggy  sources.  It  could  be  reformulated to: find the files which were mentioned in the commitswith ”fix” message most often.
3)  Find  the  most  buggy  lines  of  code.  It  could  be  reformulated to: find the lines which were mentioned in the fixing commit messages in the most buggy source files most often. 

Solving  the  first  problem  will  able  us  to  getting  know  themost regular classes of the errors in C Kernel code. 
Developer sand teachers should be aware of them by learning C techniquesand Kernel programming patterns.

Solving the second problem will show us the most unstableportions of the Kernel. Also, it could mean that a component 
corresponding  to  the  found  file  is  in  an  active  developmentand fixing because of its significance. 

And solving the third program can be useful for analysing the errors with respect to a source code.

===

I did it for fun. Please write me if you use the results of the research. 

Related publications: 

(PDF) A survey of most common errors in Linux Kernel. 
Available from: https://www.researchgate.net/publication/324834629_A_survey_of_most_common_errors_in_Linux_Kernel 

In Russian with some new data:
http://samag.ru/archive/article/3859
