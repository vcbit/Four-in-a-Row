package com.vitorpamplona.amethyst.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.BuildConfig
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.ui.components.ResizeImage
import com.vitorpamplona.amethyst.ui.components.RobohashAsyncImageProxy
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountBackupDialog
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DrawerContent(
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    sheetState: ModalBottomSheetState,
    accountViewModel: AccountViewModel
) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.background
    ) {
        Column() {
            ProfileContent(
                account.userProfile(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
                    .padding(top = 100.dp),
                scaffoldState,
                navController
            )
            Divider(
                thickness = 0.25.dp,
                modifier = Modifier.padding(top = 20.dp)
            )
            ListContent(
                account.userProfile(),
                navController,
                scaffoldState,
                sheetState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                account
            )

            BottomContent(account.userProfile(), scaffoldState, navController)
        }
    }
}

@Composable
fun ProfileContent(baseAccountUser: User, modifier: Modifier = Modifier, scaffoldState: ScaffoldState, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()

    val accountUserState by baseAccountUser.live().metadata.observeAsState()
    val accountUser = accountUserState?.user ?: return

    val accountUserFollowsState by baseAccountUser.live().follows.observeAsState()
    val accountUserFollows = accountUserFollowsState?.user ?: return

    Box {
        val banner = accountUser.info?.banner
        if (!banner.isNullOrBlank()) {
            AsyncImage(
                model = banner,
                contentDescription = stringResource(id = R.string.profile_image),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.profile_banner),
                contentDescription = stringResource(R.string.profile_banner),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }

        Column(modifier = modifier) {
            RobohashAsyncImageProxy(
                robot = accountUser.pubkeyHex,
                model = ResizeImage(accountUser.profilePicture(), 100.dp),
                contentDescription = stringResource(id = R.string.profile_image),
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .clip(shape = CircleShape)
                    .border(3.dp, MaterialTheme.colors.background, CircleShape)
                    .background(MaterialTheme.colors.background)
                    .clickable(onClick = {
                        accountUser.let {
                            navController.navigate("User/${it.pubkeyHex}")
                        }
                        coroutineScope.launch {
                            scaffoldState.drawerState.close()
                        }
                    })
            )
            if (accountUser.bestDisplayName() != null) {
                Text(
                    accountUser.bestDisplayName() ?: "",
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .clickable(onClick = {
                            accountUser.let {
                                navController.navigate("User/${it.pubkeyHex}")
                            }
                            coroutineScope.launch {
                                scaffoldState.drawerState.close()
                            }
                        }),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            if (accountUser.bestUsername() != null) {
                Text(
                    " @${accountUser.bestUsername()}",
                    color = Color.LightGray,
                    modifier = Modifier
                        .padding(top = 15.dp)
                        .clickable(onClick = {
                            accountUser.let {
                                navController.navigate("User/${it.pubkeyHex}")
                            }
                            coroutineScope.launch {
                                scaffoldState.drawerState.close()
                            }
                        })
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .clickable(onClick = {
                        accountUser.let {
                            navController.navigate("User/${it.pubkeyHex}")
                        }
                        coroutineScope.launch {
                            scaffoldState.drawerState.close()
                        }
                    })
            ) {
                Row() {
                    Text("${accountUserFollows.follows.size}", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.following))
                }
                Row(modifier = Modifier.padding(start = 10.dp)) {
                    Text("${accountUserFollows.followers.size}", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.followers))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ListContent(
    accountUser: User?,
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    sheetState: ModalBottomSheetState,
    modifier: Modifier,
    account: Account
) {
    val coroutineScope = rememberCoroutineScope()
    var backupDialogOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxHeight()) {
        if (accountUser != null) {
            NavigationRow(
                title = stringResource(R.string.profile),
                icon = Route.Profile.icon,
                tint = MaterialTheme.colors.primary,
                navCo