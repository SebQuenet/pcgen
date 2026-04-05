package pcgen.io;

import freemarker.template.Configuration;
import static freemarker.template.Configuration.VERSION_2_3_20;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import pcgen.core.GameMode;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.io.freemarker.EquipSetLoopDirective;
import pcgen.io.freemarker.LoopDirective;
import pcgen.io.freemarker.PCBooleanFunction;
import pcgen.io.freemarker.PCHasVarFunction;
import pcgen.io.freemarker.PCStringDirective;
import pcgen.io.freemarker.PCVarFunction;
import pcgen.output.publish.OutputDB;
import pcgen.util.Logging;

public class FreeMarkerExportHandler extends ExportHandler
{
	private static final Map<File, Configuration> CONFIG_CACHE = new ConcurrentHashMap<>();

	/**
	 * Constructor.  Populates the token map (a list of possible output tokens) and
	 * sets the character sheet template we are using.
	 *
	 * @param templateFile the template to use while exporting.
	 */
	FreeMarkerExportHandler(File templateFile)
	{
		super(templateFile);
	}

	private static Configuration getConfiguration(File templateDir)
	{
		return CONFIG_CACHE.computeIfAbsent(templateDir, dir -> {
			Configuration cfg = new Configuration(VERSION_2_3_20);
			try
			{
				cfg.setDirectoryForTemplateLoading(dir);
			}
			catch (IOException e)
			{
				Logging.errorPrint("Failed to set template directory: " + dir, e);
			}
			cfg.setSharedVariable("loop", new LoopDirective());
			return cfg;
		});
	}

	@Override
	public void write(PlayerCharacter aPC, BufferedWriter out) throws ExportException
	{
		File tFile = getTemplateFile();
		if (tFile==null)
		{
			Logging.errorPrint("No template file selected - aborting.");
			return;
		}
		FileAccess.setCurrentOutputFilter(getTemplateFile().getName().substring(0, getTemplateFile().getName().length() - 4));

		exportCharacterUsingFreemarker(aPC, out);
	}


	/**
	 * Produce an output file for a character using a FreeMarker template.
	 *
	 * @param aPC The character being output.
	 * @param outputWriter The destination for the output.
	 * @throws ExportException If the export fails.
	 */
	private void exportCharacterUsingFreemarker(PlayerCharacter aPC, Writer outputWriter) throws ExportException
	{
		try
		{
			Configuration cfg = getConfiguration(getTemplateFile().getParentFile());

			// load template (cached by FreeMarker's Configuration)
			Template template = cfg.getTemplate(getTemplateFile().getName());

			// Configure per-character directives and functions via the data model
			// (shared variables on cfg are shared across all threads, so per-character
			// state goes into the data model instead)
			GameMode gamemode = SettingsHandler.getGameAsProperty().get();
			Map<String, Object> pc = OutputDB.buildDataModel(aPC.getCharID());
			Map<String, Object> mode = OutputDB.buildModeDataModel(gamemode);
			Map<String, Object> input = new HashMap<>();
			input.put("pcgen", OutputDB.getGlobal());
			input.put("pc", ExportUtilities.getObjectWrapper().wrap(pc));
			input.put("gamemode", mode);
			input.put("gamemodename", gamemode.getName());
			input.put("pcstring", new PCStringDirective(aPC, this));
			input.put("pcvar", new PCVarFunction(aPC));
			input.put("pcboolean", new PCBooleanFunction(aPC, this));
			input.put("pchasvar", new PCHasVarFunction(aPC, this));
			input.put("equipsetloop", new EquipSetLoopDirective(aPC));

			// Process the template
			template.process(input, outputWriter);
		}
		catch (IOException | TemplateException exc)
		{
			String message = "Error exporting character using template " + getTemplateFile();
			Logging.errorPrint(message, exc);
			throw new ExportException(message + " : " + exc.getLocalizedMessage(), exc);
		}
		finally
		{
			if (outputWriter != null)
			{
				try
				{
					outputWriter.flush();
				}
				catch (IOException ignored)
				{
				}
			}
		}
	}

}
