/*
	https://github.com/BlackOverlord666/mslinks

	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at

	http://www.wtfpl.net/about/

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
@file:Suppress("unused", "SpellCheckingInspection")

package mslinks.data

import mslinks.ShellLinkException
import mslinks.UnsupportedCLSIDException
import java.util.*

@Suppress("JoinDeclarationAndAssignment")
object Registry {
    private val registry = ArrayList<Entry>()
    private val indexClsids = HashMap<GUID?, Entry>()
    private val indexNames = HashMap<String, Entry>()

    @Throws(ShellLinkException::class)
    fun registerClsid(clsid: GUID, name: String, vararg allowedItemIdTypes: Class<*>) {
        if (indexClsids.containsKey(clsid)) {
            throw ShellLinkException("Registry already contains $clsid")
        }
        if (indexNames.containsKey(name)) {
            throw ShellLinkException("Registry already contains $name")
        }
        registerClsidInternal(clsid, name, *allowedItemIdTypes)
    }

    private fun registerClsid(clsid: String, name: String, vararg allowedItemIdTypes: Class<*>): GUID {
        val guid = GUID(clsid)
        registerClsidInternal(guid, name, *allowedItemIdTypes)
        return guid
    }

    private fun registerClsidInternal(clsid: GUID, name: String, vararg allowedItemIdTypes: Class<*>) {
        val entry = Entry()
        entry.clsid = clsid
        entry.name = name
        if (allowedItemIdTypes.isNotEmpty()) {
            entry.allowedItemIdTypes = allowedItemIdTypes as Array<Class<*>>
        } else {
            entry.allowedItemIdTypes = arrayOf(ItemIDRegItem::class.java)
        }
        registry.add(entry)
        indexClsids[clsid] = entry
        indexNames[name.lowercase(Locale.getDefault())] = entry
    }

    @Throws(UnsupportedCLSIDException::class)
    fun getName(clsid: GUID?): String? {
        if (!indexClsids.containsKey(clsid)) {
            throw UnsupportedCLSIDException(clsid)
        }
        val entry = indexClsids[clsid]
        return entry!!.name
    }

    @Throws(ShellLinkException::class)
    fun getClsid(name: String): GUID? {
        val temp = name.lowercase(Locale.getDefault())
        if (!indexNames.containsKey(temp)) {
            throw ShellLinkException("$temp is not found")
        }
        val entry = indexNames[temp]
        return entry!!.clsid
    }

    fun canUseClsidIn(clsid: GUID?, itemIdClass: Class<*>?): Boolean {
        if (!indexClsids.containsKey(clsid)) {
            return false
        }
        val entry = indexClsids[clsid]
        for (i in entry!!.allowedItemIdTypes) {
            val check = itemIdClass?.let { i.isAssignableFrom(it) }
            if (check == true) {
                return true
            }
        }
        return false
    }

    fun asIterable(): RegistryEnumeration {
        return RegistryEnumeration()
    }

    val CLSID_COMPUTER: GUID

    @Suppress("MemberVisibilityCanBePrivate")
    val CLSID_DESKTOP: GUID

    @Suppress("MemberVisibilityCanBePrivate")
    val CLSID_DOCUMENTS: GUID

    @Suppress("MemberVisibilityCanBePrivate")
    val CLSID_DOWNLOADS: GUID


    init {
        CLSID_COMPUTER = registerClsid("{20D04FE0-3AEA-1069-A2D8-08002B30309D}", "Computer", ItemIDRoot::class.java)

        // Windows XP+
        registerClsid("{D20EA4E1-3957-11D2-A40B-0C5020524153}", "CommonAdministrativeTools")
        registerClsid("{450D8FBA-AD25-11D0-98A8-0800361B1103}", "Documents")
        registerClsid("{645FF040-5081-101B-9F08-00AA002F954E}", "RecycleBin")
        registerClsid("{D20EA4E1-3957-11D2-A40B-0C5020524152}", "Fonts") // WinXP ONLY

        // Windows 7+
        registerClsid("{D34A6CA6-62C2-4C34-8A7C-14709C1AD938}", "Links")
        registerClsid("{B155BDF8-02F0-451E-9A26-AE317CFD7779}", "NetHood")
        registerClsid("{ED50FC29-B964-48A9-AFB3-15EBB9B97F36}", "PrintHood")
        registerClsid("{4336A54D-038B-4685-AB02-99BB52D3FB8B}", "Public")
        registerClsid("{1F3427C8-5C10-4210-AA03-2EE45287D668}", "UserPinned")

        // Windows 10+
        registerClsid("{0DB7E03F-FC29-4DC6-9020-FF41B59E513A}", "3DObjects")
        CLSID_DESKTOP = registerClsid("{B4BFCC3A-DB2C-424C-B029-7FE99A87C641}", "Desktop")
        CLSID_DOWNLOADS = registerClsid("{374DE290-123F-4565-9164-39C4925E467B}", "Downloads")
        registerClsid("{D3162B92-9365-467A-956B-92703ACA08AF}", "LocalDocuments")
        registerClsid("{088E3905-0323-4B02-9826-5D99428E115F}", "LocalDownloads")
        registerClsid("{3DFDF296-DBEC-4FB4-81D1-6A3438BCF4DE}", "LocalMusic")
        registerClsid("{24AD3AD4-A569-4530-98E1-AB02F9417AA8}", "LocalPictures")
        registerClsid("{F86FA3AB-70D2-4FC7-9C99-FCBF05467F3A}", "LocalVideos")
        registerClsid("{1CF1260C-4DD0-4EBB-811F-33C572699FDE}", "MyMusic")
        registerClsid("{3ADD1653-EB32-4CB0-BBD7-DFA0ABB5ACCA}", "MyPictures")
        registerClsid("{A0953C92-50DC-43BF-BE83-3742FED03C9C}", "MyVideo")
        registerClsid("{018D5C66-4533-4307-9B53-224DE2ED1FE6}", "OneDrive")
        registerClsid("{A8CDFF1C-4878-43BE-B5FD-F8091C1C60D0}", "Personal")
        registerClsid("{F8278C54-A712-415B-B593-B77A2BE0DDA9}", "Profile")
        registerClsid("{5B934B42-522B-4C34-BBFE-37A3EF7B9C90}", "Public_1")

        //added by Griefed
        registerClsid("{59031A47-3F72-44A7-89C5-5595FE6B30EE}", "UserProfile")
        CLSID_DOCUMENTS = registerClsid("{DFD50261-23A3-0253-0400-000000004F02}", "Documents")
    }

    class RegistryEnumeration : Iterable<GUID?> {
        override fun iterator(): RegistryIterator {
            return RegistryIterator()
        }
    }

    class RegistryIterator : MutableIterator<GUID?> {
        private var idx = 0
        override fun hasNext(): Boolean {
            return idx < registry.size
        }

        override fun next(): GUID? {
            return registry[idx++].clsid
        }

        override fun remove() {

        }
    }

    private class Entry {
        var clsid: GUID? = null
        var name: String? = null
        lateinit var allowedItemIdTypes: Array<Class<*>>
    }
}
