package ac.biu.nlp.nlp.engineml.rteflow.micro.perform;

import ac.biu.nlp.nlp.codeannotations.NotThreadSafe;
import ac.biu.nlp.nlp.engineml.operations.OperationException;
import ac.biu.nlp.nlp.engineml.operations.finders.Finder;
import ac.biu.nlp.nlp.engineml.operations.operations.GenerationOperation;
import ac.biu.nlp.nlp.engineml.operations.rules.ByLemmaPosLexicalRuleBase;
import ac.biu.nlp.nlp.engineml.operations.rules.LexicalRule;
import ac.biu.nlp.nlp.engineml.operations.rules.RuleBaseEnvelope;
import ac.biu.nlp.nlp.engineml.operations.specifications.Specification;
import ac.biu.nlp.nlp.engineml.operations.updater.FeatureVectorUpdater;
import ac.biu.nlp.nlp.engineml.representation.ExtendedInfo;
import ac.biu.nlp.nlp.engineml.representation.ExtendedNode;
import ac.biu.nlp.nlp.engineml.rteflow.micro.TreesGeneratorByOperations;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.Info;
import ac.biu.nlp.nlp.instruments.parse.tree.TreeAndParentMap;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.basic.BasicNode;

/**
 * 
 * A factory of three classes: {@link Finder}, {@link GenerationOperation} and {@link FeatureVectorUpdater} -
 * all parameterized according to the appropriate {@link Specification}.
 * <P>
 * The factory is used to apply operations of a given type. For each type of operation
 * there are basically three steps to perform:
 * <ol>
 * <li>Find all the particular operations of this type that can be applied.
 * For example, if the operation type is "apply DIRT rules" - first we have to
 * find out which rules match the given tree, and for each such a rule - where it matches
 * the given tree.</li>
 * <li>Perform the operation - i.e., create a new tree which is identical to the original
 * tree but with the operation being applied. For example, is the particular operation 
 * is replacing "boy" by "child" in a specific node, then the new tree is identical
 * to the original tree, but with this specific node holding the lemma "child" instead of "boy".</li>
 * <li>Define a new feature vector that corresponds to all the operations applied on the
 * newly generated tree. The feature-vector represents all the operations that were applied
 * on the given tree - all the modifications that were performed on this tree starting from
 * the original text-parse-tree.</li>
 * </ol>
 * 
 * These three steps are done with the three classes: {@link Finder}, {@link GenerationOperation}
 * and {@link FeatureVectorUpdater}.
 * <P>
 * This class contains implementations/subclasses of the three above-mentioned classes.
 * 
 * 
 * @author Asher Stern
 * @since Jan 23, 2012
 *
 * @param <T> the type of {@link Specification} corresponds to the type of operations
 * to be applied.
 * 
 * @see TreesGeneratorByOperations
 */
@NotThreadSafe
public abstract class PerformFactory<T extends Specification>
{
	/**
	 * Returns the {@link Finder} - used to find the particular operatiosn that can be
	 * applied on a given tree.
	 * <BR>
	 * Exactly one <code>getFinder</code> method must return a non-null {@linkplain Finder}. 
	 * @param text
	 * @param hypothesis
	 * @return
	 * @throws TeEngineMlException
	 * @throws OperationException
	 */
	public abstract Finder<T> getFinder(TreeAndParentMap<ExtendedInfo, ExtendedNode> text,
			TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis) throws TeEngineMlException, OperationException;

	/**
	 * Returns the {@link Finder} - used to find the particular operatiosn that can be
	 * applied on a given tree.
	 * <BR>
	 * Exactly one <code>getFinder</code> method must return a non-null {@linkplain Finder}. 
	 * @param text
	 * @param hypothesis
	 * @param lexicalRuleBase
	 * @param ruleBaseName
	 * @return
	 * @throws TeEngineMlException
	 * @throws OperationException
	 */
	public abstract Finder<T> getFinder(TreeAndParentMap<ExtendedInfo, ExtendedNode> text,
			TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis,
			ByLemmaPosLexicalRuleBase<LexicalRule> lexicalRuleBase, String ruleBaseName) throws TeEngineMlException, OperationException;
	

	/**
	 * Returns the {@link Finder} - used to find the particular operatiosn that can be
	 * applied on a given tree.
	 * <BR>
	 * Exactly one <code>getFinder</code> method must return a non-null {@linkplain Finder}. 
	 * @param text
	 * @param hypothesis
	 * @param ruleBaseName
	 * @return
	 * @throws TeEngineMlException
	 * @throws OperationException
	 */
	public abstract Finder<T> getFinder(TreeAndParentMap<ExtendedInfo, ExtendedNode> text,
			TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis,
			String ruleBaseName) throws TeEngineMlException, OperationException;

	/**
	 * Returns the {@link Finder} - used to find the particular operatiosn that can be
	 * applied on a given tree.
	 * <BR>
	 * Exactly one <code>getFinder</code> method must return a non-null {@linkplain Finder}. 
	 * @param text
	 * @param hypothesis
	 * @param ruleBase
	 * @param ruleBaseName
	 * @return
	 * @throws TeEngineMlException
	 * @throws OperationException
	 */
	public abstract Finder<T> getFinder(TreeAndParentMap<ExtendedInfo, ExtendedNode> text,
			TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis,
			RuleBaseEnvelope<Info, BasicNode> ruleBase, String ruleBaseName) throws TeEngineMlException, OperationException;
	

	/**
	 * Returns a {@link GenerationOperation} that can apply the operation which is
	 * specified in the <code>specification</code> parameter.
	 * @param text
	 * @param hypothesis
	 * @param specification
	 * @return
	 * @throws TeEngineMlException
	 * @throws OperationException
	 */
	public abstract GenerationOperation<ExtendedInfo, ExtendedNode> getOperation(TreeAndParentMap<ExtendedInfo, ExtendedNode> text,
			TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis, T specification) throws TeEngineMlException, OperationException;
	
	/**
	 * Returns a {@link FeatureVectorUpdater} that can calculate a new feature vector
	 * for the newly created tree.
	 * @param text
	 * @param hypothesis
	 * @return
	 * @throws TeEngineMlException
	 * @throws OperationException
	 */
	public abstract FeatureVectorUpdater<T> getUpdater(TreeAndParentMap<ExtendedInfo, ExtendedNode> text,
			TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis) throws TeEngineMlException, OperationException;
}
