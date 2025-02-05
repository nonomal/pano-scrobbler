package com.arn.scrobble.info

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.databinding.ContentInfoBinding
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.divider.MaterialDividerItemDecoration
import kotlinx.coroutines.flow.filterNotNull


class InfoFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<InfoVM>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var _binding: ContentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val entry = requireArguments().getData<MusicEntry>()!!
        val username = activityViewModel.currentUser.name
        val pkgName = requireArguments().getString(Stuff.ARG_PKG)

        val adapter = InfoAdapter(
            viewModel,
            activityViewModel,
            this,
            pkgName,
        )
        binding.infoList.layoutManager = LinearLayoutManager(requireContext())
        binding.infoList.itemAnimator = null

        val itemDecor =
            MaterialDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDividerInsetStartResource(requireContext(), R.dimen.divider_inset)
                setDividerInsetEndResource(requireContext(), R.dimen.divider_inset)
                isLastItemDecorated = false
            }

        binding.infoList.addItemDecoration(itemDecor)
        binding.infoList.adapter = adapter

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            if (!it)
                binding.root.startFadeLoop()
            else
                binding.root.clearAnimation()
        }
        viewModel.setMusicEntryIfNeeded(entry, username)

        collectLatestLifecycleFlow(viewModel.infoList.filterNotNull()) {
            adapter.submitList(it) {
                scheduleTransition()
            }
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            expandIfNeeded(it)
        }
    }

}