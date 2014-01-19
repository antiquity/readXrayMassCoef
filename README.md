Read X-ray Mass Attenuation Coefficients from NIST database
===========================================================

Read X-ray mass attenuation coefficients for all materials and compositions 
automatically from the NIST database and convert into other format.

Currently the program convert all of the data to Matlab scripts, which
generates four variable:
        symbols - the short name of the materials
        zaid    - the ratio of atomic number-to-mass Z/A, the mean
                  excitation energy I, and the density ρ
        comp    - composition in terms of different elements
        mac     - mass attenuation coefficient table

There are 140 materials in total, with 92 elements and 48 compositions.

Execute the Program
===================
Compile and execute by running

        javac ReadNist.java
        java ReadNist yourMatlabFilename.m

Reference
=========
http://www.nist.gov/pml/data/xraycoef/index.cfm
