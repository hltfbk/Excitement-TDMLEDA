package ac.biu.nlp.nlp.engineml.builtin_knowledge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ac.biu.nlp.nlp.engineml.operations.rules.lexicalchain.builder.BuilderSetOfWords;
import ac.biu.nlp.nlp.engineml.rteflow.systems.ConfigurationParametersNames;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationFile;
import ac.biu.nlp.nlp.general.configuration.ConfigurationParams;
import ac.biu.nlp.nlp.lexical_resource.LexicalResource;
import ac.biu.nlp.nlp.lexical_resource.LexicalResourceException;
import ac.biu.nlp.nlp.lexical_resource.RuleInfo;

/**
 * Used to create lexical-resources to be used by {@link BuilderSetOfWords}.
 * 
 * 
 * @author Asher Stern
 * @since January 2012
 *
 */
public class ConstructorOfLexicalResourcesForChain
{
	public ConstructorOfLexicalResourcesForChain(
			ConfigurationFile configurationFile, ConfigurationParams module)
	{
		super();
		this.configurationFile = configurationFile;
		this.module = module;
	}
	
	public Map<String,LexicalResource<? extends RuleInfo>> constructResources() throws ConfigurationException, TeEngineMlException, LexicalResourceException
	{
		Map<String,LexicalResource<? extends RuleInfo>> mapResources = new LinkedHashMap<String, LexicalResource<? extends RuleInfo>>();
		List<KnowledgeResource> knowledgeResources = module.getEnumList(KnowledgeResource.class, ConfigurationParametersNames.SIMPLE_LEXICAL_CHAIN_KNOWLEDGE_RESOURCES);
		if (logger.isDebugEnabled())
		{
			logger.debug("knowledge resources:");
			for (KnowledgeResource kr : knowledgeResources)
			{logger.debug(kr.getDisplayName());}
		}
		LexicalResourcesFactory lrFactory = new LexicalResourcesFactory(this.configurationFile);
		for (KnowledgeResource  resource : knowledgeResources)
		{
			LexicalResource<? extends RuleInfo> lexicalResource = 
					lrFactory.createLexicalResource(resource);
			
			if (null==lexicalResource) throw new TeEngineMlException("Resource not supported: "+resource.getDisplayName());
			
			mapResources.put(resource.getDisplayName(), lexicalResource);
		}
		return mapResources;
	}

	private final ConfigurationFile configurationFile;
	private final ConfigurationParams module;
	
	private final static Logger logger = Logger.getLogger(ConstructorOfLexicalResourcesForChain.class);
}
