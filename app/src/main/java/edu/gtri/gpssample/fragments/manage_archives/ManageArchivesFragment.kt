package edu.gtri.gpssample.fragments.manage_archives

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ManageArchivesFragment : Fragment()
{
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View
    {
        val configs = DAO.configDAO.getArchivedConfigs()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                GPSSampleComposeTheme {
                    ManageArchivesScreen(
                        archives = configs,
                        onRestore = { config ->
                            config.isArchived = false
                            DAO.configDAO.createOrUpdateConfig( config, config.version )
                            findNavController().popBackStack()
                        },
                        onDelete = { config ->
                            DAO.configDAO.deleteConfig(config)
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.ManageArchivesFragment.value.toString() + ": " + this.javaClass.simpleName
    }
}
