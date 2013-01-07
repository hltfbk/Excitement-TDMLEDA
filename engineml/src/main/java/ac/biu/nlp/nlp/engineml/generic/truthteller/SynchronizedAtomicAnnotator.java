package ac.biu.nlp.nlp.engineml.generic.truthteller;

import ac.biu.nlp.nlp.codeannotations.ThreadSafe;
import ac.biu.nlp.nlp.engineml.representation.ExtendedNode;

/**
 * Wraps {@link SentenceAnnotator} such that the annotation is done as an
 * atomic operation in a thread safe manner.
 * This class can be shared among threads, while {@link SentenceAnnotator} is not
 * thread safe.
 * 
 * @author Asher Stern
 * @since Nov 7, 2011
 *
 */
@ThreadSafe
public class SynchronizedAtomicAnnotator
{
	public SynchronizedAtomicAnnotator(SentenceAnnotator annotator)
	{
		super();
		this.annotator = annotator;
	}

	public synchronized AnnotatedTreeAndMap annotate(ExtendedNode tree) throws AnnotatorException
	{
		annotator.setTree(tree);
		annotator.annotate();
		AnnotatedTreeAndMap ret = 
			new AnnotatedTreeAndMap(annotator.getAnnotatedTree(), annotator.getMapOriginalToAnnotated());
		
		return ret;
	}
	
	private SentenceAnnotator annotator;
}
