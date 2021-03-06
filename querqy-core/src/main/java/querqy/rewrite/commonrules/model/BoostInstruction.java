/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Map;
import java.util.Set;

import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class BoostInstruction implements Instruction {

   public enum BoostDirection {
      UP, DOWN
   }

   final QuerqyQuery<?> query;
   final BoostDirection direction;
   float boost;

   public BoostInstruction(QuerqyQuery<?> query, BoostDirection direction, float boost) {
      if (query == null) {
         throw new IllegalArgumentException("query must not be null");
      }

      if (direction == null) {
         throw new IllegalArgumentException("direction must not be null");
      }

      this.query = query;
      this.direction = direction;
      this.boost = boost;
   }

   /* (non-Javadoc)
    * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, 
    *                           querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
    */
   @Override
   public void apply(PositionSequence<Term> sequence, TermMatches termMatches,
           int startPosition, int endPosition, ExpandedQuery expandedQuery,  Map<String, Object> context) {
      BoostQuery bq = new BoostQuery(query.clone(null, true), boost);
      if (direction == BoostDirection.DOWN) {
         expandedQuery.addBoostDownQuery(bq);
      } else {
         expandedQuery.addBoostUpQuery(bq);
      }

   }
   
   @Override
   public Set<Term> getGenerableTerms() {
       return (query instanceof Query) 
           ?  TermsCollector.collect((Query) query)
           : QueryRewriter.EMPTY_GENERABLE_TERMS;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Float.floatToIntBits(boost);
      result = prime * result
            + ((direction == null) ? 0 : direction.hashCode());
      result = prime * result + ((query == null) ? 0 : query.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      BoostInstruction other = (BoostInstruction) obj;
      if (Float.floatToIntBits(boost) != Float.floatToIntBits(other.boost))
         return false;
      if (direction != other.direction)
         return false;
      if (query == null) {
         if (other.query != null)
            return false;
      } else if (!query.equals(other.query))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "BoostInstruction [query=" + query + ", direction=" + direction
            + ", boost=" + boost + "]";
   }


}
