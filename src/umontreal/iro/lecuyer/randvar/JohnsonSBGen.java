

/*
 * Class:        JohnsonSBGen
 * Description:  random variate generators for the Johnson $S_B$ distribution
 * Environment:  Java
 * Software:     SSJ
 * Copyright (C) 2001  Pierre L'Ecuyer and Universite de Montreal
 * Organization: DIRO, Universite de Montreal
 * @author
 * @since

 * SSJ is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or
 * any later version.

 * SSJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * A copy of the GNU General Public License is available at
   <a href="http://www.gnu.org/licenses">GPL licence site</a>.
 */

package umontreal.iro.lecuyer.randvar;
import umontreal.iro.lecuyer.rng.*;
import umontreal.iro.lecuyer.probdist.*;



/**
 * This class implements random variate generators for the
 * <EM>Johnson <SPAN CLASS="MATH"><I>S</I><SUB>B</SUB></SPAN></EM> distribution.
 * 
 */
public class JohnsonSBGen extends JohnsonSystemG  {


   /**
    * Creates a JohnsonSB random variate generator.
    * 
    */
   public JohnsonSBGen (RandomStream s, double gamma, double delta,
                        double xi, double lambda)  {
      super (s, new JohnsonSBDist(gamma, delta, xi, lambda));
      setParams (gamma, delta, xi, lambda);
   }


   /**
    * Creates a new generator for the JohnsonSB
    *    distribution <TT>dist</TT>, using stream <TT>s</TT>.
    * 
    */
   public JohnsonSBGen (RandomStream s, JohnsonSBDist dist)  {
      super (s, dist);
      if (dist != null)
         setParams (dist.getGamma(), dist.getDelta(), dist.getXi(),
                    dist.getLambda());
   } 


   /**
    * Uses inversion to generate a new JohnsonSB variate,
    *    using stream <TT>s</TT>.
    * 
    * 
    */
   public static double nextDouble (RandomStream s, double gamma,
                                    double delta, double xi, double lambda)  {
      return JohnsonSBDist.inverseF (gamma, delta, xi, lambda,
                                        s.nextDouble());
   }

}