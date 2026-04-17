package com.divvy.divvy.di

import com.divvy.divvy.backend.ActivityRepository
import com.divvy.divvy.backend.AndroidContactsRepository
import com.divvy.divvy.backend.AuthRepository
import com.divvy.divvy.backend.BalanceRepository
import com.divvy.divvy.backend.ContactsRepository
import com.divvy.divvy.backend.ForexRepository
import com.divvy.divvy.backend.FrankfurterForexRepository
import com.divvy.divvy.backend.ExpensesRepository
import com.divvy.divvy.backend.FriendsRepository
import com.divvy.divvy.backend.GroupRepository
import com.divvy.divvy.backend.MemberRepository
import com.divvy.divvy.backend.ProfilesRepository
import com.divvy.divvy.backend.SupabaseAuthRepository
import com.divvy.divvy.offline.repository.OfflineFriendsRepository
import com.divvy.divvy.backend.DefaultStatementRepository
import com.divvy.divvy.backend.StatementRepository
import com.divvy.divvy.backend.SupabaseProfilesRepository
import com.divvy.divvy.offline.repository.OfflineActivityRepository
import com.divvy.divvy.offline.repository.OfflineBalanceRepository
import com.divvy.divvy.offline.repository.OfflineExpensesRepository
import com.divvy.divvy.offline.repository.OfflineGroupRepository
import com.divvy.divvy.offline.repository.OfflineMemberRepository
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
