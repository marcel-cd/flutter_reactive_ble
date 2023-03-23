import 'uuid.dart';

class ScanSession {
  final List<Uuid> withServices;
  final List<int> withCompanyIds;
  final Future<void> future;

  const ScanSession({required this.withServices, required this.withCompanyIds, required this.future});
}
