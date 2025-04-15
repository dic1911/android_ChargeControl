package moe.hx030.chargecontrol

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.hx030.chargecontrol.databinding.FragmentMainBinding
import java.lang.Integer.parseInt

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var myActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        myActivity = (activity as MainActivity)
        myActivity.setFragment(this)

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    fun refresh(auto: Boolean) {
        if (!auto) {
            binding.chargeStartValue.setText(myActivity.startLevel)
            binding.chargeStopValue.setText(myActivity.stopLevel)
        }
        binding.batteryCycleValue.setText(myActivity.cycles)
        try {
            binding.batteryStatusValue.text = if (!myActivity.isCharging) myActivity.status
//                else "${myActivity.status} (${myActivity.chargeType} - done: ${myActivity.chargeDone})"
                else "${myActivity.status} (${myActivity.chargeType})"

            binding.batteryTempValue.text = "${((parseInt(myActivity.temp).toFloat()) / 10)} ℃"

            val current = parseInt(myActivity.current).toFloat() / 1000
            val currentAvg = parseInt(myActivity.currentAvg).toFloat() / 1000
            binding.batteryCurrentValue.text = "${current}mA\n(Avg. ${currentAvg}mA)"

            val voltage = parseInt(myActivity.voltage).toFloat() / 1000000
//            val voltageAvg = parseInt(myActivity.voltageAvg).toFloat() / 1000000
//            binding.batteryVoltageValue.text = "${voltage}V\n(Avg. ${voltageAvg}V)"
            binding.batteryVoltageValue.text = "${voltage}V"

            val full = parseInt(myActivity.fullCapacity.trim()) / 1000
            val fullDesign = parseInt(myActivity.fullCapacityDesign.trim()) / 1000
            binding.batteryCapacityValue.setText("${full}mAh (Design: ${fullDesign}mAh) - ${(full * 100) / fullDesign}%")
        } catch (ex: Exception) {
            Log.e("030-cc", "error rendering data", ex)
            myActivity.snack(ex.message.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in 0..1) myActivity.writeValue(i, null)

        binding.buttonApply.setOnClickListener {
            if (!myActivity.hasSUAccess) myActivity.snack(getString(R.string.no_root))
            for (i in 0..1) {
                val value = if (i == 0) binding.chargeStartValue.text.toString().trim() else binding.chargeStopValue.text.toString().trim()
                if (value.isNotBlank()) myActivity.writeValue(i, value)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}