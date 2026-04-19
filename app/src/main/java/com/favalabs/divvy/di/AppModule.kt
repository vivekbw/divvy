package com.favalabs.divvy.di

import com.favalabs.divvy.backend.ActivityRepository
import com.favalabs.divvy.backend.AndroidContactsRepository
import com.favalabs.divvy.backend.AuthRepository
import com.favalabs.divvy.backend.BalanceRepository
import com.favalabs.divvy.backend.ContactsRepository
import com.favalabs.divvy.backend.ForexRepository
import com.favalabs.divvy.backend.FrankfurterForexRepository
import com.favalabs.divvy.backend.ExpensesRepository
import com.favalabs.divvy.backend.FriendsRepository
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.MemberRepository
import com.favalabs.divvy.backend.ProfilesRepository
import com.favalabs.divvy.backend.SupabaseAuthRepository
import com.favalabs.divvy.offline.repository.OfflineFriendsRepository
import com.favalabs.divvy.backend.DefaultStatementRepository
import com.favalabs.divvy.backend.StatementRepository
import com.favalabs.divvy.backend.SupabaseProfilesRepository
import com.favalabs.divvy.offline.repository.OfflineActivityRepository
import com.favalabs.divvy.offline.repository.OfflineBalanceRepository
import com.favalabs.divvy.offline.repository.OfflineExpensesRepository
import com.favalabs.divvy.offline.repository.OfflineGroupRepository
import com.favalabs.divvy.offline.repository.OfflineMemberRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository
    @Binds @Singleton abstract fun bindGroupRepository(impl: OfflineGroupRepository): GroupRepository
    @Binds @Singleton abstract fun bindMemberRepository(impl: OfflineMemberRepository): MemberRepository
    @Binds @Singleton abstract fun bindBalanceRepository(impl: OfflineBalanceRepository): BalanceRepository
    @Binds @Singleton abstract fun bindExpensesRepository(impl: OfflineExpensesRepository): ExpensesRepository
    @Binds @Singleton abstract fun bindProfilesRepository(impl: SupabaseProfilesRepository): ProfilesRepository
    @Binds @Singleton abstract fun bindActivityRepository(impl: OfflineActivityRepository): ActivityRepository
    @Binds @Singleton abstract fun bindStatementRepository(impl: DefaultStatementRepository): StatementRepository
    @Binds @Singleton abstract fun bindFriendsRepository(impl: OfflineFriendsRepository): FriendsRepository
    @Binds @Singleton abstract fun bindContactsRepository(impl: AndroidContactsRepository): ContactsRepository
    @Binds @Singleton abstract fun bindForexRepository(impl: FrankfurterForexRepository): ForexRepository
}
