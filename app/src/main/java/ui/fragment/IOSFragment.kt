package ui.fragment

import android.os.Bundle
import android.support.v4.app.Fragment

/**
 * Created by heweizong on 2017/6/19.
 */
class IOSFragment:Fragment() {
    companion object {
        val IOS = "iOS"
        fun newInstance(): IOSFragment {
            val fragment = IOSFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }
}